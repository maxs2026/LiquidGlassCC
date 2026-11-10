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
import androidx.lifecycle.ViewTreeLifecycleOwner
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
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showEdgeHandle() {
        if (edgeHandle != null || overlayView != null) return
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(0x2EFFFFFF)
                setStroke(dp(1), 0x55FFFFFF)
            }
            var downY = 0f
            var downX = 0f
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downY = event.rawY
                        downX = event.rawX
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = event.rawY - downY
                        val deltaX = abs(event.rawX - downX)
                        if (deltaY >= dp(64) && deltaY > deltaX * 1.15f) showOverlay()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> true
                    else -> true
                }
            }
        }
        val params = WindowManager.LayoutParams(
            dp(24),
            dp(142),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = dp(18)
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
        ViewTreeLifecycleOwner.set(composeView, this)
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
        private const val CHANNEL_ID = "liquidglass_overlay"
        private const val NOTIFICATION_ID = 2601
    }
}
