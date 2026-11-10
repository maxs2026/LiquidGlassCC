package com.maxs2026.liquidglasscc

import android.app.Notification
import android.app.NotificationChannel
import android.os.Build
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewTreeLifecycleOwner

class OverlayService : LifecycleService() {
    private var overlayView: ComposeView? = null
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
            .setContentText("Tap the + or power icon to close the floating Control Center.")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        showOverlay()
    }

    private fun showOverlay() {
        if (overlayView != null) return
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

    override fun onDestroy() {
        overlayView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    companion object {
        private const val CHANNEL_ID = "liquidglass_overlay"
        private const val NOTIFICATION_ID = 2601
    }
}
