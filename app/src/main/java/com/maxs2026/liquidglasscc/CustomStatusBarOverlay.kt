package com.maxs2026.liquidglasscc

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * A small, transparent iOS-inspired status strip. This is an accessibility
 * overlay so it can be drawn above app content/system-bar area where Android
 * permits it; it does not modify Android's actual SystemUI status bar.
 */
class CustomStatusBarOverlay(private val service: AccessibilityService) {
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = service.getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE)
    private var root: LinearLayout? = null
    private var clock: TextView? = null
    private var battery: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var downY = 0f\n    private var swipeTriggered = false

    private val refresh = object : Runnable {
        override fun run() {
            clock?.text = SimpleDateFormat(if (prefs.getBoolean(KEY_24H, false)) "HH:mm" else "h:mm", Locale.getDefault()).format(Date())
            battery?.text = "▰ ${batteryLevel()}%"
            handler.postDelayed(this, 30_000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            battery?.text = "▰ ${batteryLevel()}%"
        }
    }

    fun show() {
        if (root != null || !prefs.getBoolean(KEY_ENABLED, false)) return
        val height = dp(prefs.getInt(KEY_BAR_HEIGHT, 28).coerceIn(20, 44))
        val alpha = prefs.getInt(KEY_BAR_ALPHA, 0).coerceIn(0, 100) / 100f
        val bar = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(Color.argb((alpha * 255).toInt(), 12, 16, 25))
        }
        val left = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        clock = TextView(service).apply {
            setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
        }
        left.addView(clock)
        val notifications = TextView(service).apply {
            text = "  ●  ●"; setTextColor(Color.WHITE); textSize = 9f
            visibility = if (prefs.getBoolean(KEY_NOTIFICATIONS, true)) View.VISIBLE else View.GONE
        }
        left.addView(notifications)
        bar.addView(left, LinearLayout.LayoutParams(0, -1, 1f))

        val right = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val signal = TextView(service).apply {
            text = "▂▄▆"; setTextColor(Color.WHITE); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            visibility = if (prefs.getBoolean(KEY_SIGNAL, true)) View.VISIBLE else View.GONE
        }
        val wifi = TextView(service).apply {
            text = "  ◉"; setTextColor(Color.WHITE); textSize = 13f
            visibility = if (prefs.getBoolean(KEY_WIFI, true)) View.VISIBLE else View.GONE
        }
        battery = TextView(service).apply {
            text = "▰ ${batteryLevel()}%"; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
            visibility = if (prefs.getBoolean(KEY_BATTERY, true)) View.VISIBLE else View.GONE
        }
        right.addView(signal); right.addView(wifi); right.addView(battery)
        bar.addView(right)

        bar.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = event.rawY; swipeTriggered = false; true }
                MotionEvent.ACTION_MOVE -> {
                    if (event.rawY - downY > dp(prefs.getInt(OverlayService.KEY_SWIPE, 64).coerceIn(24, 160))) {
                        service.startService(Intent(service, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW))
                        true
                    } else true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(prefs.getInt(KEY_BAR_TOP, 0).coerceIn(0, 16)) }
        try {
            wm.addView(bar, params)
            root = bar
            handler.post(refresh)
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (Build.VERSION.SDK_INT >= 33) service.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("DEPRECATION") service.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            android.util.Log.e("LiquidGlassCC", "Status strip attach failed", e)
            root = null
        }
    }

    fun refresh() { hide(); show() }

    fun hide() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        handler.removeCallbacks(refresh)
        try { service.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
    }

    private fun batteryLevel(): Int {
        val intent = service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra("level", 100) ?: 100
        val scale = intent?.getIntExtra("scale", 100) ?: 100
        return (level * 100 / scale.coerceAtLeast(1)).coerceIn(0, 100)
    }
    private fun dp(v: Int) = (v * service.resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_REFRESH = "com.maxs2026.liquidglasscc.action.REFRESH_STATUS_BAR"\n        const val KEY_ENABLED = "custom_status_enabled"
        const val KEY_BAR_HEIGHT = "custom_status_height"
        const val KEY_BAR_ALPHA = "custom_status_alpha"
        const val KEY_BAR_TOP = "custom_status_top"
        const val KEY_24H = "custom_status_24h"
        const val KEY_NOTIFICATIONS = "custom_status_notifications"
        const val KEY_SIGNAL = "custom_status_signal"
        const val KEY_WIFI = "custom_status_wifi"
        const val KEY_BATTERY = "custom_status_battery"
    }
}
