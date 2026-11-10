package com.maxs2026.liquidglasscc

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Registered accessibility entry for LiquidGlass CC.
 *
 * The swipe handle itself is managed by OverlayService using the overlay
 * permission. This service is intentionally passive and does not inspect
 * screen contents or intercept touch input.
 */
class LiquidGlassAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen content is collected or processed.
    }

    override fun onInterrupt() {
        // Nothing to interrupt; this service does not run an interaction.
    }
}
