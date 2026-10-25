package app.intentional.pause.monitor

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.content.*
import android.content.res.Configuration
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import app.intentional.pause.IntentApp
import app.intentional.pause.core.SessionMachine
import app.intentional.pause.core.UsageMath
import app.intentional.pause.data.LiveState
import kotlinx.coroutines.*
import java.time.LocalDate

class MonitorService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store get() = (application as IntentApp).store
    private val machine = SessionMachine()
    private lateinit var overlays: Overlays
    private lateinit var usage: UsageSource
    private var lastQuery = 0L
    private var checkpointAt = 0L
    private val windowPackages = mutableMapOf<Int, String>()
    private var lastAccessibilityAt = 0L
    private var lastForeground: String? = null
    private var ready = false
    private var overlayError: String? = null
    private var keyboards = emptySet<String>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> transition { machine.leave() }
                Intent.ACTION_USER_PRESENT -> { lastQuery = System.currentTimeMillis() - 2000; reconcile() }
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> { store.checkpoint(); lastQuery = System.currentTimeMillis(); render() }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && machine.app == intent.data?.schemeSpecificPart) transition { machine.leave() }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (ready) return
        overlays = Overlays(this, ::leaveToHome, ::approve)
        usage = UsageSource(this)
        usage.recover(store)
        keyboards = getSystemService(InputMethodManager::class.java).inputMethodList.map { it.packageName }.toSet()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        registerReceiver(receiver, IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") })
        ready = true
        store.live.value = LiveState(connected = true)
        lastQuery = System.currentTimeMillis()
        if (allowed()) {
            val recent = usage.events(lastQuery - 86_400_000L, lastQuery)
            val resumed = recent.lastOrNull { it.type == UsageEvents.Event.ACTIVITY_RESUMED }
            if (resumed != null && recent.none { it.time >= resumed.time &&
                    (it.type == UsageEvents.Event.ACTIVITY_PAUSED && it.pkg == resumed.pkg ||
                     it.type in setOf(UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN, UsageEvents.Event.DEVICE_SHUTDOWN)) }) {
                lastForeground = resumed.pkg
                transition { machine.foreground(resumed.pkg, monitored(resumed.pkg)) }
            }
        }
        scope.launch {
            store.prefs.collect { prefs ->
                if (!prefs.enabled || prefs.apps.none { it.pkg == machine.app && it.enabled }) transition { machine.leave() }
                else render()
            }
        }
        scope.launch {
            while (isActive) {
                reconcile()
                if (SystemClock.elapsedRealtime() - checkpointAt >= 5000) {
                    store.checkpoint(); checkpointAt = SystemClock.elapsedRealtime()
                }
                render()
                delay(if (machine.app != null) 750 else 3000)
            }
        }
    }

    private fun usable() = getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
    private fun allowed() = store.prefs.value.enabled && Access.usage(this) && usable()
    private fun transient(pkg: String) = pkg == "android" || pkg.contains("permissioncontroller") || pkg.contains("packageinstaller") || pkg == "com.android.systemui"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!ready || event == null) return
        if (!allowed()) { transition { machine.leave() }; return }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) { readWindowFocus(); return }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Do not access event text, source nodes, window contents, or typed input.
        val pkg = event.packageName?.toString() ?: return
        if (event.windowId >= 0) windowPackages[event.windowId] = pkg
        if (pkg in keyboards) { readWindowFocus(); return }
        if (pkg == packageName && event.className?.toString() != "app.intentional.pause.MainActivity") return
        lastAccessibilityAt = System.currentTimeMillis()
        if (transient(pkg)) {
            transition { machine.interrupt() }
            readWindowFocus()
            return
        }
        if (packageManager.getLaunchIntentForPackage(pkg) == null && pkg != packageName) return
        lastForeground = pkg
        transition { machine.foreground(pkg, monitored(pkg)) }
        readWindowFocus()
    }

    /** Only window metadata is inspected. No root, source, text, or screen node is requested. */
    private fun readWindowFocus() {
        if (!ready || !allowed()) return
        val visible = windows.filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        val focused = visible.firstOrNull { it.isFocused }
            ?: visible.firstOrNull { it.isActive }
            ?: visible.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && windowPackages[it.id] == machine.app }
        if (focused == null) { transition { machine.interrupt() }; return }
        if (focused.type != AccessibilityWindowInfo.TYPE_APPLICATION || focused.isInPictureInPictureMode) {
            transition { machine.interrupt() }; return
        }
        val pkg = windowPackages[focused.id] ?: lastForeground?.takeIf {
            visible.count { window -> window.type == AccessibilityWindowInfo.TYPE_APPLICATION } == 1
        }?.also { windowPackages[focused.id] = it } ?: return
        if (pkg in keyboards) return
        if (transient(pkg)) { transition { machine.interrupt() }; return }
        if (pkg == packageName && machine.prompting) return
        lastForeground = pkg
        transition { machine.foreground(pkg, monitored(pkg)) }
        val ids = visible.map { it.id }.toSet()
        windowPackages.keys.retainAll(ids)
    }

    private fun monitored(pkg: String) = pkg != packageName && store.prefs.value.apps.any { it.pkg == pkg && it.enabled }

    private fun reconcile() {
        val now = System.currentTimeMillis()
        if (!allowed()) { lastQuery = now; transition { machine.leave() }; return }
        val events = usage.events(lastQuery, now)
        lastQuery = now
        events.forEach { event ->
            // Accessibility delivers transitions first. Older UsageStats events must not undo them.
            if (event.time < lastAccessibilityAt) return@forEach
            when (event.type) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (event.pkg in keyboards || transient(event.pkg)) return@forEach
                    lastForeground = event.pkg
                    transition { machine.foreground(event.pkg, monitored(event.pkg)) }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> if (event.pkg == machine.app && !machine.prompting) {
                    transition { machine.interrupt() }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> transition { machine.leave() }
            }
        }
        readWindowFocus()
    }

    private fun transition(action: () -> Unit) {
        val wasCounting = machine.counting
        val old = machine.app
        action()
        if (wasCounting && (!machine.counting || old != machine.app)) store.stop()
        if (machine.counting && (!wasCounting || old != machine.app)) store.start(machine.app!!)
        render()
    }

    private fun approve(answer: String) {
        if (!allowed() || store.prefs.value.answers.none { it.id == answer }) return
        transition { machine.approve() }
    }

    private fun leaveToHome() {
        transition { machine.leave() }
        lastForeground = null
        lastAccessibilityAt = System.currentTimeMillis()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun render() {
        if (!ready) return
        val permission = Access.usage(this)
        if (overlayError != null) {
            store.live.value = LiveState(true, error = overlayError)
            return
        }
        try {
            val prefs = store.prefs.value
            when {
                !prefs.enabled || !permission || !usable() -> overlays.hide()
                machine.prompting -> overlays.warning(machine.app!!, prefs.apps.firstOrNull { it.pkg == machine.app }?.label ?: "this app", prefs.answers)
                machine.counting && prefs.timer -> overlays.timer("Today's usage: ${UsageMath.clock(store.days(1)[LocalDate.now()] ?: 0)}", prefs.timerOffset)
                else -> overlays.hide()
            }
            store.live.value = LiveState(true, machine.app, machine.counting, if (!permission) "Usage Access is off. Monitoring is paused." else null)
        } catch (_: RuntimeException) {
            store.stop(); machine.leave(); overlays.hide()
            overlayError = "The overlay could not be displayed. Toggle Intent off and on in Accessibility settings."
            store.live.value = LiveState(true, error = overlayError)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (ready) { overlays.resize(); render() }
    }
    override fun onInterrupt() { if (ready) transition { machine.interrupt() } }
    override fun onUnbind(intent: Intent?): Boolean { shutdown(); return super.onUnbind(intent) }
    override fun onDestroy() { shutdown(); super.onDestroy() }
    private fun shutdown() {
        if (!ready) return
        ready = false
        store.stop(); machine.leave(); overlays.hide(); scope.cancel()
        runCatching { unregisterReceiver(receiver) }
        store.live.value = LiveState()
    }
}
