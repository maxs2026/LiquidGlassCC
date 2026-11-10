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
class LiquidGlassAccessibilityService : AccessibilityService() {\n    private var statusOverlay: CustomStatusBarOverlay? = null\n\n    override fun onServiceConnected() {\n        super.onServiceConnected()\n        statusOverlay = CustomStatusBarOverlay(this).also { it.show() }\n    }\n\n    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {\n        super.onConfigurationChanged(newConfig)\n        statusOverlay?.refresh()\n    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen content is collected or processed.
    }

    override fun onInterrupt() {
        // Nothing to interrupt; this service does not run an interaction.
    }
}
