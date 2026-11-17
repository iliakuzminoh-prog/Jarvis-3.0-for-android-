package com.urchi.jarvis

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import kotlin.math.max

/**
 * User-authorized UI controller.
 *
 * It never enables Accessibility itself. Once the user explicitly enables the
 * service, commands can inspect the active window, fuzzy-match visible labels,
 * click their clickable ancestors, scroll containers and perform global actions.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityServiceInfo.DEFAULT
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 80
            isAccessibilityTool = true
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        lastWindowPackage = event?.packageName?.toString()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun clickBest(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = normalize(query)
        if (target.isBlank()) return false
        val nodes = mutableListOf<NodeScore>()
        collect(root, target, nodes)
        val best = nodes.maxByOrNull { it.score }?.node ?: return false
        return clickNode(best)
    }

    /**
     * Handles commands such as "нажми видео из рекомендаций" by using the
     * meaningful words in the phrase rather than requiring an exact label.
     */
    fun clickByIntent(query: String): Boolean {
        val q = normalize(query)
        val candidates = listOf(
            q,
            q.replace(Regex("\b(нажми|кликни|тыкни|выбери|открой|включи|запусти|на|из|в|по|рекомендаци[яи])\b"), " ")
                .replace(Regex("\s+"), " ").trim(),
            when {
                q.contains("рекомендац") && q.contains("видео") -> "видео рекомендации"
                q.contains("рекомендац") && q.contains("музык") -> "музыка рекомендации"
                q.contains("музык") || q.contains("песн") || q.contains("трек") -> "музыка"
                q.contains("видео") || q.contains("ролик") -> "видео"
                else -> q
            }
        ).filter { it.isNotBlank() }.distinct()

        for (candidate in candidates) {
            if (clickBest(candidate)) return true
        }
        return false
    }

    fun scrollForward(): Boolean = rootInActiveWindow?.let {
        val node = findScrollable(it) ?: return false
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    } ?: false

    fun scrollBackward(): Boolean = rootInActiveWindow?.let {
        val node = findScrollable(it) ?: return false
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    } ?: false

    fun globalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun globalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun globalRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun globalNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun globalQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun tapFocused(): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return false
        return clickNode(focused)
    }

    private data class NodeScore(val node: AccessibilityNodeInfo, val score: Int)

    private fun collect(
        node: AccessibilityNodeInfo,
        target: String,
        out: MutableList<NodeScore>,
        depth: Int = 0
    ) {
        if (depth > 40) return
        val label = normalize(node.text?.toString().orEmpty())
        val desc = normalize(node.contentDescription?.toString().orEmpty())
        val viewId = normalize(node.viewIdResourceName?.substringAfterLast('/').orEmpty())
        val combined = listOf(label, desc, viewId).filter { it.isNotBlank() }.joinToString(" ")
        if (combined.isNotBlank()) {
            val score = fuzzyScore(target, combined, node)
            if (score >= 28) out += NodeScore(node, score)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collect(child, target, out, depth + 1)
                // Do not recycle children obtained from a live root hierarchy.
            }
        }
    }

    private fun fuzzyScore(target: String, candidate: String, node: AccessibilityNodeInfo): Int {
        if (candidate == target) return 140
        if (candidate.contains(target)) return 120
        val tw = target.split(' ').filter { it.length >= 2 }.toSet()
        val cw = candidate.split(' ').filter { it.length >= 2 }.toSet()
        var score = tw.count { word -> cw.any { it == word || it.contains(word) || word.contains(it) } } * 24
        if (tw.size > 1 && tw.all { word -> candidate.contains(word) }) score += 35
        if (node.isClickable) score += 18
        if (node.isFocusable) score += 4
        val distance = editDistance(target.replace(" ", ""), candidate.take(80).replace(" ", ""))
        if (target.replace(" ", "").length >= 4 && distance <= max(2, target.length / 4)) {
            score += 28 - distance * 4
        }
        return score
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(8) {
            val n = current ?: return false
            if (n.isClickable && n.isEnabled) {
                if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            current = n.parent
        }

        // Some custom UIs expose no clickable node but do expose bounds. A
        // gesture is still user-authorized through the Accessibility service.
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && serviceInfo.flags >= 0) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                val path = android.graphics.Path().apply { moveTo(x, y) }
                val gesture = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(
                            path, 0, 80
                        )
                    ).build()
                return dispatchGesture(gesture, null, null)
            }
        }
        return false
    }

    private fun findScrollable(node: AccessibilityNodeInfo, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 40) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollable(child, depth + 1)?.let { return it }
        }
        return null
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(Regex("[^\p{L}\p{N} ]"), " ")
            .replace(Regex("\s+"), " ")
            .trim()

    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val cur = IntArray(b.length + 1)
            cur[0] = i + 1
            for (j in b.indices) {
                cur[j + 1] = minOf(
                    cur[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            prev = cur
        }
        return prev[b.length]
    }

    companion object {
        @Volatile var instance: JarvisAccessibilityService? = null
            private set
        @Volatile var lastWindowPackage: String? = null
            private set

        fun isServiceEnabled(context: android.content.Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val expected = context.packageName + "/" + JarvisAccessibilityService::class.java.name
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun isEnabled(context: android.content.Context): Boolean = isServiceEnabled(context)
    }
}
