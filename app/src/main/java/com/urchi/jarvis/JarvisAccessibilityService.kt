package com.urchi.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return clickRecursive(root, text.trim())
    }

    private fun clickRecursive(node: AccessibilityNodeInfo, text: String): Boolean {
        val target = text.trim().take(200).lowercase()
        if (target.isBlank()) return false
        val label = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (label == target || desc == target) {
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.parent?.let { if (it.isClickable) return it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickRecursive(child, text)) return true
        }
        return false
    }

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        fun isServiceEnabled(context: android.content.Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val expected = context.packageName + "/" + JarvisAccessibilityService::class.java.name
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun isEnabled(context: android.content.Context): Boolean =
            isServiceEnabled(context)
    }
}
