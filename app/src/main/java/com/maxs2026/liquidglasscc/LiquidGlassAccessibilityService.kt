package com.maxs2026.liquidglasscc

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility entry used to draw the optional status strip above app content.
 * It does not inspect screen content or collect accessibility events.
 */
class LiquidGlassAccessibilityService : AccessibilityService() {
    private var statusOverlay: CustomStatusBarOverlay? = null
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { statusOverlay?.refresh() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        statusOverlay = CustomStatusBarOverlay(this).also { it.show() }
        val filter = IntentFilter(CustomStatusBarOverlay.ACTION_REFRESH)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(refreshReceiver, filter)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        statusOverlay?.refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        statusOverlay?.hide()
        statusOverlay = null
        try { unregisterReceiver(refreshReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
