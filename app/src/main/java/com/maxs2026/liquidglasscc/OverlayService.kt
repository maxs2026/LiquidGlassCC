package com.maxs2026.liquidglasscc

import android.app.Notification
import android.app.NotificationChannel
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import kotlin.math.abs

class OverlayService : LifecycleService() {
    private var overlayView: ComposeView? = null
    private var edgeHandle: View? = null
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "LiquidGlass overlay", android.app.NotificationManager.IMPORTANCE_LOW)
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = notificationBuilder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("LiquidGlass CC is active")
            .setContentText("Swipe down from the upper-right edge to open Control Center.")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        showEdgeHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> {
                hideOverlay()
                showEdgeHandle()
            }
            ACTION_REFRESH_SETTINGS -> {
                removeEdgeHandle()
                showEdgeHandle()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showEdgeHandle() {
        if (edgeHandle != null || overlayView != null) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val location = prefs.getString(KEY_LOCATION, "RIGHT") ?: "RIGHT"
        val hue = prefs.getInt(KEY_HUE, 210).toFloat()
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 1f))
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(android.graphics.Color.argb(72, android.graphics.Color.red(rgb), android.graphics.Color.green(rgb), android.graphics.Color.blue(rgb)))
                setStroke(dp(1), android.graphics.Color.argb(125, android.graphics.Color.red(rgb), android.graphics.Color.green(rgb), android.graphics.Color.blue(rgb)))
            }
            var downX = 0f
            var downY = 0f
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = event.rawX - downX
                        val deltaY = event.rawY - downY
                        val threshold = dp(prefs.getInt(KEY_SWIPE, 64))
                        val triggered = when (location) {
                            "TOP" -> deltaY >= threshold && deltaY > abs(deltaX) * 1.15f
                            "BOTTOM" -> -deltaY >= threshold && -deltaY > abs(deltaX) * 1.15f
                            "LEFT" -> deltaX >= threshold && deltaX > abs(deltaY) * 1.15f
                            else -> -deltaX >= threshold && -deltaX > abs(deltaY) * 1.15f
                        }
                        if (triggered) showOverlay()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> true
                    else -> true
                }
            }
        }
        val edgeWidth = dp(prefs.getInt(KEY_WIDTH, 24))
        val edgeHeight = dp(prefs.getInt(KEY_HEIGHT, 142))
        val horizontal = location == "TOP" || location == "BOTTOM"
        val params = WindowManager.LayoutParams(
            if (horizontal) edgeHeight else edgeWidth,
            if (horizontal) edgeWidth else edgeHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = when (location) {
                "TOP" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                "BOTTOM" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                "LEFT" -> Gravity.CENTER_VERTICAL or Gravity.START
                else -> Gravity.CENTER_VERTICAL or Gravity.END
            }
            if (location == "TOP" || location == "BOTTOM") y = dp(18)
            if (location == "LEFT" || location == "RIGHT") x = dp(2)
        }
        edgeHandle = handle
        windowManager.addView(handle, params)
    }

    private fun showOverlay() {
        if (overlayView != null) return
        removeEdgeHandle()
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IOSControlCenter(isOverlay = true) }
        }
        composeView.setViewTreeLifecycleOwner(this)
        val windowFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            dimAmount = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled) {
                setBlurBehindRadius(48)
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            }
        }
        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        overlayView = null
    }

    private fun removeEdgeHandle() {
        edgeHandle?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        edgeHandle = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        hideOverlay()
        removeEdgeHandle()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    companion object {
        const val ACTION_SHOW = "com.maxs2026.liquidglasscc.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.maxs2026.liquidglasscc.action.HIDE_OVERLAY"
        const val ACTION_REFRESH_SETTINGS = "com.maxs2026.liquidglasscc.action.REFRESH_SETTINGS"
        const val PREFS = "liquidglass_gesture_settings"
        const val KEY_WIDTH = "edge_width_dp"
        const val KEY_SWIPE = "swipe_distance_dp"
        const val KEY_HEIGHT = "edge_height_dp"
        const val KEY_LOCATION = "edge_location"
        const val KEY_HUE = "edge_hue"
        private const val CHANNEL_ID = "liquidglass_overlay"
        private const val NOTIFICATION_ID = 2601
    }
}
