package com.urchi.jarvis

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class JarvisPermissionCenter(private val context: Context) {
    data class Item(val id: String, val title: String, val description: String, val granted: Boolean, val action: () -> Unit)

    fun items(activity: Activity): List<Item> = listOf(
        Item("microphone", "Микрофон", "Распознавание голосовых команд.",
            has(Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 4101)
        },
        Item("camera", "Камера", "Просмотр фотографий и работа с камерой.",
            has(Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 4102)
        },
        Item("notifications", "Уведомления", "Фоновый помощник и уведомления.",
            Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= 33)
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4103)
        },
        Item("overlay", "Плавающее окно", "Показывает JARVIS поверх других приложений.",
            Settings.canDrawOverlays(context)) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)))
        },
        Item("accessibility", "Управление кликами",
            "Опциональный Android AccessibilityService для действий с интерфейсом.",
            JarvisAccessibilityService.isEnabled(context)) {
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    )

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
