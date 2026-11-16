package com.urchi.jarvis

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermissionCenterActivity : AppCompatActivity() {
    private lateinit var content: LinearLayout
    private val accent = 0xFF53D7C2.toInt()
    private val background = 0xFF0B1118.toInt()
    private val panel = 0xFF121D28.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "JARVIS — доступы"
        val scroll = ScrollView(this).apply { setBackgroundColor(this@PermissionCenterActivity.background) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(24))
        }
        scroll.addView(content)
        setContentView(scroll)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }

    private fun render() {
        content.removeAllViews()
        addText("J A R V I S", 25f, accent, true, Gravity.CENTER)
        addText("ЦЕНТР УПРАВЛЕНИЯ РАЗРЕШЕНИЯМИ", 11f, 0xFFB7C5D1.toInt(), true, Gravity.CENTER)
        addText(
            "JARVIS может открыть системную страницу нужного доступа, но включить его вместо пользователя Android не позволяет. Галочка появляется только после повторной проверки.",
            13f, 0xFFD6E0E8.toInt(), false, Gravity.START
        )
        addButton("ЗАПРОСИТЬ ОСНОВНЫЕ РАЗРЕШЕНИЯ") { requestCorePermissions() }
        addPermission("Уведомления", "Напоминания и состояние фоновой службы",
            if (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) "Разрешено" else "Не разрешено",
            Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        addPermission("Микрофон", "Голосовые команды и распознавание речи",
            if (granted(Manifest.permission.RECORD_AUDIO)) "Разрешено" else "Не разрешено",
            granted(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 102)
        }
        addPermission("Камера", "Фонарик и функции прямой съёмки; выбор фотографии для анализа работает через системный выбор файлов",
            if (granted(Manifest.permission.CAMERA)) "Разрешено" else "Не разрешено",
            granted(Manifest.permission.CAMERA)) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 103)
        }
        addPermission("Bluetooth", "Разрешение на чтение состояния и запрос подключения Bluetooth. Само включение подтверждается системным окном.", 
            if (Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_CONNECT)) "Разрешено" else "Не разрешено",
            Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (Build.VERSION.SDK_INT >= 31 && !granted(Manifest.permission.BLUETOOTH_CONNECT)) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 104)
            }
        }
        addPermission("Будильники", "Сейчас JARVIS передаёт создание будильника стандартному приложению «Часы». Специальный доступ точных будильников для этого не нужен.", "Через системные Часы", true) {
            runCatching { startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM)) }
                .onFailure { Toast.makeText(this, "Не найдено приложение Часы.", Toast.LENGTH_SHORT).show() }
        }
        addPermission("Показ поверх приложений", "Плавающая кнопка и окно JARVIS",
            if (Settings.canDrawOverlays(this)) "Разрешено" else "Требуется настройка",
            Settings.canDrawOverlays(this)) { openOverlaySettings() }
        addPermission("Специальные возможности", "Выполнение команд: Домой, Назад, шторка, жесты и поиск элементов по запросу",
            if (isAccessibilityEnabled()) "Включено" else "Требуется настройка",
            isAccessibilityEnabled()) { openAccessibilitySettings() }
        addPermission("Работа без ограничений батареи", "Может уменьшить остановку фоновых задач; не гарантирует постоянную работу",
            if (ignoresBatteryOptimizations()) "Разрешено" else "Не настроено",
            ignoresBatteryOptimizations()) { openBatterySettings() }
        addButton("ОТКРЫТЬ НАСТРОЙКИ УВЕДОМЛЕНИЙ JARVIS") {
            runCatching {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }.onFailure { openAppDetails() }
        }
        addButton("ОТКРЫТЬ СТРАНИЦУ ПРИЛОЖЕНИЯ") { openAppDetails() }
        addText(
            "Приватность: JARVIS не получает SMS, журнал звонков, контакты или полный доступ ко всем файлам. Для документов используется системный выбор файлов. Доступ Accessibility даёт широкие возможности управления экраном — включай его только если действительно пользуешься этими командами.",
            12f, 0xFF9EADB9.toInt(), false, Gravity.START
        )
    }

    private fun requestCorePermissions() {
        val needed = mutableListOf<String>()
        if (!granted(Manifest.permission.RECORD_AUDIO)) needed += Manifest.permission.RECORD_AUDIO
        if (!granted(Manifest.permission.CAMERA)) needed += Manifest.permission.CAMERA
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) {
            render()
            return
        }
        requestPermissions(needed.toTypedArray(), 100)
    }

    private fun granted(permission: String): Boolean =
        Build.VERSION.SDK_INT < 23 || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun openOverlaySettings() {
        if (Settings.canDrawOverlays(this)) return
        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }.onFailure { openAppDetails() }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, JarvisAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .onFailure { openAppDetails() }
    }

    private fun ignoresBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        val manager = getSystemService(POWER_SERVICE) as PowerManager
        return manager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openBatterySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { openAppDetails() }
    }

    private fun openAppDetails() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun addPermission(name: String, description: String, status: String, enabled: Boolean, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            setBackgroundColor(panel)
        }
        val heading = TextView(this).apply {
            text = (if (enabled) "✓  " else "○  ") + name + "  ·  " + status
            textSize = 14f
            setTextColor(if (enabled) accent else 0xFFFFC56D.toInt())
        }
        card.addView(heading)
        card.addView(TextView(this).apply {
            text = description
            textSize = 11f
            setTextColor(0xFFB7C5D1.toInt())
        })
        val button = Button(this).apply {
            text = if (enabled) "Проверить / открыть" else "Настроить"
            isAllCaps = false
            setOnClickListener { action() }
        }
        card.addView(button)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun addButton(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(this@PermissionCenterActivity.background)
            setBackgroundColor(accent)
            setOnClickListener { action() }
        }
        content.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8); bottomMargin = dp(8)
        })
    }

    private fun addText(value: String, size: Float, color: Int, bold: Boolean, gravityValue: Int) {
        val view = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(2), dp(8), dp(2), dp(8))
        }
        content.addView(view, LinearLayout.LayoutParams(-1, -2))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
