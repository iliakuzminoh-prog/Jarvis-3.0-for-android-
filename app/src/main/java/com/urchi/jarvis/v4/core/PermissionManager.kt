package com.urchi.jarvis.v4.core

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

class PermissionManager(private val context: Context) {
    data class Capability(
        val id: String,
        val title: String,
        val rationale: String,
        val permissions: List<String>,
        val requiresOverlaySettings: Boolean = false
    )

    companion object {
        const val REQ_MICROPHONE = 4101
        const val REQ_CAMERA = 4102
        const val REQ_NOTIFICATIONS = 4103
        const val REQ_BLUETOOTH = 4104

        val MICROPHONE = Capability(
            "microphone", "Голос",
            "JARVIS использует микрофон только для распознавания вашей команды.",
            listOf(Manifest.permission.RECORD_AUDIO)
        )
        val CAMERA = Capability(
            "camera", "Камера",
            "Камера нужна только когда вы просите JARVIS посмотреть изображение.",
            listOf(Manifest.permission.CAMERA)
        )
        val NOTIFICATIONS = Capability(
            "notifications", "Уведомления",
            "Нужны для уведомлений, таймеров и фонового сервиса.",
            if (Build.VERSION.SDK_INT >= 33)
                listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        )
        val BLUETOOTH = Capability(
            "bluetooth", "Bluetooth",
            "Нужно только для команд, связанных с Bluetooth.",
            if (Build.VERSION.SDK_INT >= 31)
                listOf(Manifest.permission.BLUETOOTH_CONNECT)
            else emptyList()
        )
        val OVERLAY = Capability(
            "overlay", "Плавающее окно",
            "Android требует отдельного системного разрешения для плавающей кнопки JARVIS.",
            emptyList(), true
        )
    }

    fun missing(capability: Capability): List<String> =
        capability.permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun granted(capability: Capability): Boolean =
        missing(capability).isEmpty() &&
            (!capability.requiresOverlaySettings || Settings.canDrawOverlays(context))

    fun request(activity: Activity, capability: Capability): Boolean {
        val missingPermissions = missing(capability)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity, missingPermissions.toTypedArray(), requestCode(capability)
            )
            return false
        }
        if (capability.requiresOverlaySettings && !Settings.canDrawOverlays(context)) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                )
            )
            return false
        }
        return true
    }

    fun requestCode(capability: Capability): Int = when (capability.id) {
        "microphone" -> REQ_MICROPHONE
        "camera" -> REQ_CAMERA
        "notifications" -> REQ_NOTIFICATIONS
        "bluetooth" -> REQ_BLUETOOTH
        else -> 4199
    }

    fun capabilityForRequestCode(code: Int): Capability? = when (code) {
        REQ_MICROPHONE -> MICROPHONE
        REQ_CAMERA -> CAMERA
        REQ_NOTIFICATIONS -> NOTIFICATIONS
        REQ_BLUETOOTH -> BLUETOOTH
        else -> null
    }
}
