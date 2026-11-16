package com.urchi.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return clickRecursive(root, text.trim())
    }

    private fun clickRecursive(node: AccessibilityNodeInfo, text: String): Boolean {
        val target = text.lowercase()
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
        fun isEnabled(context: android.content.Context): Boolean {
            val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager
            return manager.isEnabled
        }
    }
}
