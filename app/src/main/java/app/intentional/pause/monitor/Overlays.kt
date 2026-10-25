package app.intentional.pause.monitor

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.*
import app.intentional.pause.data.Answer
import app.intentional.pause.ui.IntentTheme
import app.intentional.pause.ui.WarningScreen

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
    init {
        saved.performAttach(); saved.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun dispose() { registry.currentState = Lifecycle.State.DESTROYED }
}

class Overlays(private val service: AccessibilityService, private val leave: () -> Unit, private val approve: (String) -> Unit) {
    private val windows = service.getSystemService(WindowManager::class.java)
    private var warning: FrameLayout? = null
    private var owner: OverlayOwner? = null
    private var timer: TextView? = null
    private var warningKey: String? = null
    private var back: OnBackInvokedCallback? = null
    private var backDispatcher: OnBackInvokedDispatcher? = null
    private fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()

    fun warning(pkg: String, label: String, answers: List<Answer>) {
        hideTimer()
        val key = pkg + answers.toString()
        if (warningKey == key) return
        hideWarning()
        val lifecycleOwner = OverlayOwner()
        owner = lifecycleOwner
        val root = object : FrameLayout(service) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) leave()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        root.setViewTreeLifecycleOwner(lifecycleOwner)
        root.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        root.setBackgroundColor(Color.BLACK)
        root.addView(ComposeView(service).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { IntentTheme { WarningScreen(label, answers, approve, leave) } }
        }, FrameLayout.LayoutParams(-1, -1))
        val bounds = windows.currentWindowMetrics.bounds
        val insets = windows.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        val cutout = windows.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout())
        root.setPadding((cutout.left - insets.left).coerceAtLeast(0), maxOf(insets.top, cutout.top), (cutout.right - insets.right).coerceAtLeast(0), 0)
        val params = WindowManager.LayoutParams(bounds.width() - insets.left - insets.right, bounds.height() - insets.bottom, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.OPAQUE)
        params.gravity = Gravity.TOP or Gravity.START
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        params.x = insets.left
        params.title = "Intent intention prompt"
        params.setFitInsetsTypes(0)
        windows.addView(root, params)
        warning = root
        warningKey = key
        if (android.os.Build.VERSION.SDK_INT >= 33) root.post {
            if (warning !== root) return@post
            back = OnBackInvokedCallback { leave() }
            backDispatcher = root.findOnBackInvokedDispatcher()
            backDispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, back!!)
        }
    }

    fun timer(text: String, offset: Int) {
        hideWarning()
        val view = timer ?: TextView(service).apply {
            setTextColor(Color.rgb(217, 255, 238))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(13), dp(7), dp(13), dp(7))
            background = GradientDrawable().apply { setColor(Color.rgb(15, 43, 36)); cornerRadius = dp(24).toFloat(); setStroke(dp(1), Color.rgb(75, 113, 96)) }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
        }.also { view ->
            val params = WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT)
            params.gravity = Gravity.TOP or Gravity.END
            params.x = dp(12); params.y = dp(offset)
            params.title = "Intent daily usage"
            params.setFitInsetsTypes(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            windows.addView(view, params)
            timer = view
        }
        view.text = text
        val params = view.layoutParams as WindowManager.LayoutParams
        if (params.y != dp(offset)) { params.y = dp(offset); windows.updateViewLayout(view, params) }
    }

    fun hideWarning() {
        if (android.os.Build.VERSION.SDK_INT >= 33) back?.let { backDispatcher?.unregisterOnBackInvokedCallback(it) }
        back = null; backDispatcher = null
        warning?.let { runCatching { windows.removeViewImmediate(it) } }
        warning = null; warningKey = null
        owner?.dispose(); owner = null
    }
    fun hideTimer() { timer?.let { runCatching { windows.removeViewImmediate(it) } }; timer = null }
    fun resize() {
        warning?.let { root ->
            val metrics = windows.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val cutout = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout())
            root.setPadding((cutout.left - insets.left).coerceAtLeast(0), maxOf(insets.top, cutout.top), (cutout.right - insets.right).coerceAtLeast(0), 0)
            val params = root.layoutParams as WindowManager.LayoutParams
            params.width = metrics.bounds.width() - insets.left - insets.right
            params.height = metrics.bounds.height() - insets.bottom
            params.x = insets.left
            windows.updateViewLayout(root, params)
        }
    }
    fun hide() { hideWarning(); hideTimer() }
}
