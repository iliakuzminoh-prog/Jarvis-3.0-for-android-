package com.urchi.jarvis

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var root: LinearLayout
    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val accent = Color.rgb(83, 215, 194)
    private val panel = Color.rgb(18, 29, 40)
    private val installedApps = mutableListOf<AppEntry>()

    data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        loadApps()
        renderApps("")
        say("Система запущена. Чем могу помочь?")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts.language = Locale("ru", "RU")
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(11,17,24)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)

        val brand = TextView(this).apply {
            text = "J A R V I S"
            textSize = 25f
            letterSpacing = .18f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(brand, matchWrap())
        val subtitle = TextView(this).apply {
            text = "ЛОКАЛЬНЫЙ МОБИЛЬНЫЙ ПОМОЩНИК"
            textSize = 10f
            letterSpacing = .12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, matchWrap())
        addSpace(20)

        output = TextView(this).apply {
            text = "Готов к работе. Команды выполняются на устройстве."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(panel)
        }
        root.addView(output, matchWrap())
        addSpace(12)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Например: открой YouTube"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            singleLine = true
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(panel)
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        val mic = button("МИК", false) { startVoice() }
        row.addView(mic, LinearLayout.LayoutParams(dp(64), dp(52)))
        root.addView(row, matchWrap())
        addSpace(8)
        root.addView(button("ВЫПОЛНИТЬ КОМАНДУ", true) { runCommand(input.text.toString()) }, matchWrap())
        addSpace(18)

        section("БЫСТРЫЕ ДЕЙСТВИЯ")
        val actions = listOf(
            "Открыть настройки" to "настройки",
            "Wi-Fi" to "открой настройки wi-fi",
            "Bluetooth" to "открой bluetooth",
            "Включить VPN" to "включи vpn",
            "Текущее время" to "который час",
            "Помощь" to "помощь"
        )
        actions.chunked(2).forEach { pair ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { item ->
                line.addView(button(item.first, false) { runCommand(item.second) },
                    LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(6), dp(6)) })
            }
            root.addView(line, matchWrap())
        }

        addSpace(12)
        section("ПРИЛОЖЕНИЯ НА ТЕЛЕФОНЕ")
        val search = EditText(this).apply {
            hint = "Поиск приложений..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(panel)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString() ?: "") }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        addSpace(8)
        val appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "app_list" }
        root.addView(appList, matchWrap())
        addSpace(20)
        val footer = TextView(this).apply {
            text = "Работает локально • Без API-ключей • Действия требуют системных разрешений"
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        root.addView(footer, matchWrap())
    }

    private fun section(text: String) {
        val t = TextView(this).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            setPadding(0, dp(4), 0, dp(10))
        }
        root.addView(t, matchWrap())
    }

    private fun button(label: String, primary: Boolean, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(if (primary) Color.rgb(5, 24, 27) else Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (primary) accent else panel))
            setOnClickListener { action() }
        }
    }

    private fun loadApps() {
        installedApps.clear()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        list.mapNotNull { info ->
            try {
                AppEntry(info.loadLabel(packageManager).toString(), info.activityInfo.packageName, info.loadIcon(packageManager))
            } catch (_: Exception) { null }
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }.forEach { installedApps.add(it) }
    }

    private fun renderApps(filter: String) {
        val list = root.findViewWithTag<LinearLayout>("app_list") ?: return
        list.removeAllViews()
        val matches = installedApps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
        matches.take(100).forEach { app ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(7), dp(10), dp(7))
                setBackgroundColor(panel)
                val icon = ImageView(this@MainActivity).apply { setImageDrawable(app.icon) }
                addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))
                val name = TextView(this@MainActivity).apply {
                    text = app.label
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), 0, 0, 0)
                }
                addView(name, LinearLayout.LayoutParams(0, dp(44), 1f))
                setOnClickListener { launchApp(app.packageName); respond("Открываю ${app.label}") }
            }
            val lp = LinearLayout.LayoutParams(-1, dp(54))
            lp.bottomMargin = dp(5)
            list.addView(row, lp)
        }
        if (matches.isEmpty()) list.addView(TextView(this).apply { text = "Ничего не найдено"; setTextColor(Color.GRAY) })
    }

    private fun runCommand(raw: String) {
        val cmd = raw.trim().lowercase(Locale.getDefault())
        if (cmd.isBlank()) { respond("Напиши или произнеси команду."); return }
        input.setText("")
        try {
            when {
                cmd in listOf("помощь", "что ты умеешь", "команды") -> respond(helpText())
                cmd.contains("впн") || cmd.contains("vpn") -> openVpn()
                cmd.contains("wi-fi") || cmd.contains("вайфай") || cmd.contains("вай фай") -> openSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Открываю настройки Wi-Fi.")
                cmd.contains("bluetooth") || cmd.contains("блютуз") -> openSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Открываю настройки Bluetooth.")
                cmd.contains("настройк") -> openSettings(android.provider.Settings.ACTION_SETTINGS, "Открываю настройки.")
                cmd.contains("который час") || cmd == "время" || cmd.contains("сколько времени") -> respond("Сейчас ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}.")
                cmd.contains("какое сегодня число") || cmd.contains("дата") -> respond("Сегодня ${SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())}.")
                cmd.contains("найди") || cmd.contains("поищи") || cmd.contains("загугли") -> {
                    val query = cmd.replace(Regex("^(найди|поищи|загугли)\\s*"), "")
                    openWeb(query.ifBlank { raw })
                }
                cmd.startsWith("открой ") || cmd.startsWith("запусти ") || cmd.startsWith("включи ") -> {
                    val target = cmd.substringAfter(" ").trim()
                    val app = installedApps.firstOrNull { it.label.equals(target, true) }
                        ?: installedApps.firstOrNull { it.label.contains(target, true) && target.length >= 3 }
                    if (app != null) { launchApp(app.packageName); respond("Запускаю ${app.label}.") }
                    else if (target.contains("браузер")) openWeb("")
                    else if (target.contains("камер")) openSettings("android.media.action.IMAGE_CAPTURE", "Открываю камеру.")
                    else { respond("Не нашёл приложение «$target». Посмотри список приложений ниже или уточни название.") }
                }
                cmd.contains("интернет") -> openWeb("")
                else -> {
                    val candidate = installedApps.firstOrNull { it.label.contains(cmd, true) && cmd.length >= 3 }
                    if (candidate != null) { launchApp(candidate.packageName); respond("Похоже, ты имел в виду ${candidate.label}. Открываю.") }
                    else respond("Я не нашёл готовую команду для этого запроса. Могу открыть приложение, системные настройки или выполнить поиск в интернете. Скажи «помощь» для списка.")
                }
            }
        } catch (e: Exception) { respond("Не удалось выполнить действие: ${e.localizedMessage ?: "ошибка Android"}.") }
    }

    private fun openVpn() {
        val vpnWords = listOf("vpn", "впн", "wireguard", "openvpn", "tunnel", "прокси")
        val candidates = installedApps.filter { app ->
            (app.label + " " + app.packageName).lowercase().let { s -> vpnWords.any { s.contains(it) } }
        }
        if (candidates.isEmpty()) {
            respond("Не нашёл установленный VPN-клиент. Открываю настройки VPN Android.")
            openSettings("android.settings.VPN_SETTINGS", "Открой настройки VPN и выбери установленный клиент.")
            return
        }
        val preferred = candidates.sortedBy { app ->
            val s = (app.label + app.packageName).lowercase()
            when {
                s.contains("wireguard") -> 0
                s.contains("proton") -> 1
                s.contains("openvpn") -> 2
                s.contains("1.1.1.1") || s.contains("warp") -> 3
                else -> 4
            }
        }
        val first = preferred.first()
        try {
            launchApp(first.packageName)
            respond("Запустил ${first.label}. Android требует подтвердить подключение внутри VPN-клиента. Я не могу достоверно проверить подключение без разрешения самого VPN. Если клиент не соединится, вернись сюда и скажи «следующий VPN».")
        } catch (_: Exception) {
            val next = preferred.drop(1).firstOrNull()
            if (next != null) { launchApp(next.packageName); respond("Первый клиент не открылся. Запускаю ${next.label}.") }
            else { openSettings("android.settings.VPN_SETTINGS", "Не удалось открыть VPN-клиенты. Открываю системные настройки VPN.") }
        }
    }

    private fun launchApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: throw ActivityNotFoundException()
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
    }

    private fun openSettings(action: String, message: String) {
        try { startActivity(Intent(action)); respond(message) }
        catch (_: Exception) { respond("Эти настройки недоступны на данной версии Android.") }
    }

    private fun openWeb(query: String) {
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        respond(if (query.isBlank()) "Открываю браузер." else "Ищу в интернете: $query")
    }

    private fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите команду")
        }
        try { startActivityForResult(intent, 41) }
        catch (_: ActivityNotFoundException) { respond("На телефоне нет доступного сервиса распознавания речи.") }
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == Activity.RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { input.setText(spoken); runCommand(spoken) }
        }
    }

    private fun helpText() = "Я умею: открывать установленные приложения по названию; показывать их значки; открывать Wi-Fi, Bluetooth и системные настройки; запускать найденный VPN-клиент; сообщать время и дату; искать в интернете; принимать голосовые команды. Это локальный набор правил, а не обученная большая языковая модель."

    private fun respond(message: String) {
        output.text = message
        say(message)
    }

    private fun say(message: String) {
        if (ttsReady) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun addSpace(size: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(size))) }
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
