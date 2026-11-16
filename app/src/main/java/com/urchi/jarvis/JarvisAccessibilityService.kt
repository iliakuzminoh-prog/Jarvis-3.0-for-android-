package com.urchi.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Local, user-enabled accessibility bridge.
 * It does not collect, upload, or persist screen contents.
 * Actions are exposed only to code running inside this application.
 */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not inspect or store event text.
    }

    override fun onInterrupt() = Unit

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun clickVisibleText(query: String): Boolean {
        if (query.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        return clickMatchingNode(root, query.trim(), 0)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return scrollNode(root, 0)
    }

    private fun clickMatchingNode(node: AccessibilityNodeInfo, query: String, depth: Int): Boolean {
        if (depth > 30) return false
        val label = node.text?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        if ((label.contains(query, ignoreCase = true) || description.contains(query, ignoreCase = true))
            && node.isVisibleToUser && node.isEnabled) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            val parent = node.parent
            if (parent != null && parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickMatchingNode(child, query, depth + 1)) return true
        }
        return false
    }

    private fun scrollNode(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 30) return false
        if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (scrollNode(child, depth + 1)) return true
        }
        return false
    }

    companion object {
        @Volatile var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
