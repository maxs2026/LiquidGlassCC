package com.maxs2026.liquidglasscc

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility entry used to draw the optional status strip above app content.
 * It does not inspect screen content or collect accessibility events.
 */
class LiquidGlassAccessibilityService : AccessibilityService() {
    private var statusOverlay: CustomStatusBarOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        statusOverlay = CustomStatusBarOverlay(this).also { it.show() }
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
        super.onDestroy()
    }
}
