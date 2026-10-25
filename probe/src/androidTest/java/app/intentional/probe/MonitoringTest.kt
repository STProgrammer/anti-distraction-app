package app.intentional.probe

import android.app.UiAutomation
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** External instrumentation leaves Intent's actual service process running. */
@RunWith(AndroidJUnit4::class)
class MonitoringTest {
    private val automation = InstrumentationRegistry.getInstrumentation().getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).apply {
        serviceInfo = serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
    }
    private val first = "app.intentional.probe.one"
    private val second = "app.intentional.probe.two"
    private fun shell(command: String): String {
        val result = automation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText() }
        if (command.startsWith("am start")) Thread.sleep(700)
        return result
    }
    private fun until(message: String, predicate: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < end) {
            if (predicate()) return
            Thread.sleep(100)
        }
        run {
            automation.windows.forEach { window ->
                val root = window.root
                println("Window metadata: ${window.title}, type=${window.type}, package=${root?.packageName}, children=${root?.childCount}")
                if (root?.packageName?.toString() == "app.intentional.pause") {
                    fun list(node: AccessibilityNodeInfo, depth: Int) {
                        if (depth > 8) return
                        if (node.text != null) println("Intent control: ${node.text}")
                        repeat(node.childCount) { node.getChild(it)?.let { child -> list(child, depth + 1) } }
                    }
                    list(root, 0)
                }
            }
        }
        fail(message)
    }
    private fun findNode(match: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        automation.clearCache()
        fun search(current: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (match(current)) return current
            repeat(current.childCount) { current.getChild(it)?.let { child -> search(child)?.let { found -> return found } } }
            return null
        }
        return automation.windows.mapNotNull { it.root }.filter { it.packageName?.toString() in setOf("app.intentional.pause", first, second) }
            .firstNotNullOfOrNull { search(it) }
    }
    private fun node(text: String) = findNode { it.text?.toString().equals(text, ignoreCase = true) || it.hintText?.toString() == text || it.contentDescription?.toString() == text }
    private fun screenshot(name: String) {
        val destination = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "$name.png")
        destination.outputStream().use { stream -> automation.takeScreenshot()?.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream) }
    }
    private fun tap(text: String) {
        runCatching { automation.waitForIdle(150, 2000) }
        if (text == "Settings" && node("A gentler default.") != null) return
        if (text == "Today" && node("TODAY'S SOCIAL TIME") != null) return
        until("Visible control: $text") {
            val match = node(text) ?: return@until false
            val bounds = android.graphics.Rect()
            match.getBoundsInScreen(bounds)
            if (bounds.isEmpty) return@until false
            shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
            true
        }
        Thread.sleep(400)
    }
    private fun toggle(text: String, checked: Boolean) {
        until("Switch is visible: $text") { node(text) != null }
        if (node(text)?.isChecked != checked) tap(text)
        until("Switch $text = $checked") { node(text)?.isChecked == checked }
    }
    private fun open(pkg: String) { shell("am start -W -n $pkg/app.intentional.probe.ProbeActivity") }
    private fun prompt() { until("Intention prompt is visible") { automation.windows.any { it.title?.toString() == "Intent intention prompt" } } }
    // Non-touchable windows are intentionally absent from the accessibility tree.
    private fun timer() = shell("dumpsys window windows").contains("Intent daily usage")
    private fun approve() { tap("To use messenger"); tap("Continue"); until("Prompt dismissed and live timer shown") { node("Why are you opening this app?") == null && timer() } }

    @Test fun promptSwitchKeyboardShadeHomeAndBack() {
        open(first); prompt()
        screenshot("warning")
        assertFalse("Continue initially disabled", node("Continue")?.parent?.isEnabled ?: true)
        approve()
        screenshot("timer")
        tap("Keyboard check")
        Thread.sleep(1000)
        assertNull("Keyboard does not prompt again", node("Why are you opening this app?"))
        assertTrue("Keyboard preserves timer", timer())
        shell("input keyevent 4")
        tap("Show temporary dialog"); tap("Done")
        assertNull("App dialog preserves intention", node("Why are you opening this app?"))
        shell("cmd statusbar expand-notifications"); Thread.sleep(700)
        until("Shade hides timer") { !timer() }
        shell("cmd statusbar collapse"); Thread.sleep(700)
        assertNull("Shade preserves intention", node("Why are you opening this app?"))
        until("Closing shade restores timer") { timer() }
        open(second); prompt(); approve()
        open(first); prompt(); approve()
        shell("input keyevent 3"); Thread.sleep(500); open(first); prompt()
        shell("input keyevent 4")
        until("Back exits safely") { node("Why are you opening this app?") == null }
        open(second); prompt(); tap("Leave this app")
        until("Leave exits safely") { node("Why are you opening this app?") == null }
    }

    private fun dashboardTotal(): Long {
        shell("am start -W -n app.intentional.pause/.MainActivity")
        tap("Today")
        val pattern = Regex("[0-9]{2,}:[0-9]{2}:[0-9]{2}")
        var text: String? = null
        until("Dashboard total displayed") { text = findNode { pattern.matches(it.text?.toString().orEmpty()) }?.text?.toString(); text != null }
        val parts = text!!.split(':').map { it.toLong() }
        return parts[0] * 3600 + parts[1] * 60 + parts[2]
    }

    @Test fun accountingTogglesAndRotation() {
        shell("input keyevent 3")
        shell("am start -W -n app.intentional.pause/.MainActivity")
        tap("Settings"); toggle("Global monitoring", true); toggle("Live timer visibility", true)
        val before = dashboardTotal()
        open(first); prompt(); Thread.sleep(2500); approve(); Thread.sleep(1400)
        open(second); prompt(); Thread.sleep(2500); approve(); Thread.sleep(1400)
        val difference = dashboardTotal() - before
        assertTrue("Combined usage excludes five seconds spent on prompts: $difference seconds", difference in 2..6)
        tap("Settings"); toggle("Global monitoring", false)
        open(first); Thread.sleep(1500)
        assertNull("Disabled monitoring never prompts", node("Why are you opening this app?"))
        assertFalse("Disabled monitoring hides timer", timer())
        shell("am start -W -n app.intentional.pause/.MainActivity")
        tap("Settings"); toggle("Global monitoring", true); toggle("Live timer visibility", false)
        open(first); prompt(); tap("To use messenger"); tap("Continue")
        until("Hidden timer leaves app usable") { node("Why are you opening this app?") == null }
        assertFalse("Timer visibility switch applied", timer())
        shell("am start -W -n app.intentional.pause/.MainActivity")
        tap("Settings"); toggle("Live timer visibility", true)
        open(first); prompt()
        try {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
            Thread.sleep(900)
            prompt()
            screenshot("warning-landscape")
            repeat(4) {
                if (node("To use messenger")?.isVisibleToUser != true) {
                    findNode { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Thread.sleep(200)
                }
            }
            tap("To use messenger"); tap("Continue")
            until("Landscape session starts") { timer() }
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
            Thread.sleep(900)
            assertNull("Rotation preserves approval", node("Why are you opening this app?"))
            assertTrue("Timer survives rotation", timer())
        } finally { automation.setRotation(UiAutomation.ROTATION_UNFREEZE); shell("input keyevent 3") }
    }

    @Test fun serviceProcessRecovery() {
        shell("input keyevent 3")
        open(first); prompt(); approve(); Thread.sleep(1200)
        val pid = shell("pidof app.intentional.pause").trim()
        assertTrue("Known Intent process", pid.matches(Regex("[0-9]+")))
        shell("run-as app.intentional.pause kill -9 $pid")
        prompt()
        approve()
        shell("input keyevent 3")
        until("Recovered service still follows Home") { !timer() }
    }

    @Test fun recentsEndsTheApprovedSession() {
        shell("input keyevent 3")
        open(first); prompt(); approve()
        shell("input keyevent 187")
        until("Recents hides timer") { !timer() }
        open(first); prompt()
        tap("Leave this app")
    }
}
