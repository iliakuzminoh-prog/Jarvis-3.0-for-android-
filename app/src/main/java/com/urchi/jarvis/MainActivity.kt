package com.urchi.jarvis

import android.app.Activity
import android.provider.AlarmClock
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.content.Context
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
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
    private var vpnCandidates: List<AppEntry> = emptyList()
    private var vpnIndex = -1
    private val commandStack = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences("jarvis_local", Context.MODE_PRIVATE) }

    data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        loadApps()
        renderApps("")
        say("Система запущена. Чем могу помочь?")
        handleOverlayIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOverlayIntent(intent)
    }

    private fun handleOverlayIntent(intent: Intent?) {
        if (intent == null) return
        val command = intent.getStringExtra(EXTRA_OVERLAY_COMMAND)
        val voice = intent.getBooleanExtra(EXTRA_OVERLAY_VOICE, false)
        if (!command.isNullOrBlank()) root.post { runCommand(command) }
        else if (voice) root.post { startVoice() }
        intent.removeExtra(EXTRA_OVERLAY_COMMAND)
        intent.removeExtra(EXTRA_OVERLAY_VOICE)
    }

    private fun enableOverlay() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            respond("Для прозрачной работы плавающего помощника нужно разрешить его постоянное уведомление.")
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 53)
            return
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            respond("Для плавающего окна нужно разрешить JARVIS показываться поверх других приложений.")
            val permissionIntent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try { startActivityForResult(permissionIntent, 52) }
            catch (_: Exception) { respond("Открой настройки приложений и вручную разреши показ поверх других окон.") }
            return
        }
        try {
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
            respond("Плавающая кнопка JARVIS включена. Дважды нажми на неё, чтобы открыть окно запроса.")
        } catch (_: Exception) {
            respond("Не удалось запустить плавающее окно. Проверь разрешения и уведомления JARVIS.")
        }
    }

    private fun disableOverlay() {
        stopService(Intent(this, OverlayService::class.java))
        respond("Плавающая кнопка JARVIS выключена.")
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false
            return
        }
        val languageStatus = tts.setLanguage(Locale("ru", "RU"))
        val localRussianVoice = tts.voices
            ?.filter { it.locale.language == "ru" && !it.isNetworkConnectionRequired }
            ?.maxByOrNull { it.quality }
        if (localRussianVoice != null) tts.voice = localRussianVoice
        tts.setSpeechRate(1.07f)
        tts.setPitch(1.02f)
        ttsReady = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
            languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
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
        addSpace(14)
        root.addView(button("ПЛАВАЮЩЕЕ ОКНО JARVIS", false) { enableOverlay() }, matchWrap())
        addSpace(20)

        output = TextView(this).apply {
            text = "Готов к работе. Команды выполняются на устройстве."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground(panel, 16)
            elevation = dp(3).toFloat()
        }
        root.addView(output, matchWrap())
        addSpace(12)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Например: открой YouTube"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            isSingleLine = true
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedBackground(panel, 12)
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
            "Батарея" to "покажи заряд батареи",
            "Память" to "сколько свободной памяти",
            "Пауза музыки" to "пауза",
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
            background = roundedBackground(panel, 12)
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
            minHeight = dp(44)
            elevation = dp(2).toFloat()
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
        list.addView(TextView(this).apply {
            text = "Найдено: ${matches.size} • отображается: ${minOf(matches.size, 100)}"
            textSize = 11f
            setTextColor(Color.GRAY)
            setPadding(dp(4), dp(4), dp(4), dp(8))
        })
        matches.take(100).forEach { app ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(7), dp(10), dp(7))
                background = roundedBackground(panel, 12)
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
        if (handleConversation(cmd)) return
        if (commandStack.size >= 8 || !commandStack.add(cmd)) {
            respond("Обнаружен цикл или слишком глубокая цепочка команд. Остановил выполнение.")
            return
        }
        try {
            val learned = prefs.getString("alias_" + cmd, null)
            if (learned != null && learned != cmd) {
                respond("Выполняю сохранённую команду.")
                runCommand(learned)
                return
            }
            if (cmd.contains(" потом ")) {
                val parts = cmd.split(" потом ").map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size > 5) { respond("Одна цепочка может содержать не больше пяти шагов."); return }
                respond("Выполняю последовательность из ${parts.size} действий.")
                parts.forEach { runCommand(it) }
                return
            }
            when {
                cmd.contains("впн") || cmd.contains("vpn") -> openVpn()
                cmd == "адгуард" || cmd == "adguard" || cmd == "открой адгуард" || cmd == "запусти адгуард" -> openAdGuardVpn()
                cmd == "открой ии" || cmd == "запусти ии" || cmd == "включи ии" || cmd == "открой ai" || cmd == "запусти ai" -> openAiRandom()
                cmd == "открой чат джпт" || cmd == "открой чатжпт" || cmd == "открой chatgpt" || cmd == "запусти чат джпт" || cmd == "запусти chatgpt" || cmd == "открой джпт" -> openChatGpt()
                cmd == "открой клод" || cmd == "открой claude" || cmd == "запусти клод" || cmd == "запусти claude" -> openClaude()
                cmd.contains("выключи плавающее окно") || cmd.contains("убери кнопку jarvis") -> disableOverlay()
                cmd.contains("плавающее окно") || cmd.contains("включи плавающий jarvis") -> enableOverlay()
                cmd.startsWith("научи команду ") -> teachCommand(raw)
                cmd.startsWith("удали команду ") -> deleteAlias(raw)
                cmd.contains("мои команды") || cmd.contains("покажи команды") -> showAliases()
                cmd.contains("очисти заметки") || cmd.contains("удали все заметки") -> confirmClearNotes()
                cmd.startsWith("запомни ") || cmd.startsWith("сохрани заметку ") -> saveNote(raw)
                cmd.contains("мои заметки") || cmd.contains("покажи заметки") || cmd == "заметки" -> showNotes()
                cmd.contains("таймер") -> setTimer(raw)
                cmd.contains("будильник") -> openAlarm(raw)
                cmd.contains("календар") || cmd.contains("событие") -> openCalendar()
                cmd.contains("фонарик") -> toggleTorch(cmd.contains("выключ"))
                cmd.contains("заряд") || cmd.contains("батаре") -> showBattery()
                cmd.contains("информация о телефоне") || cmd.contains("характеристики телефона") -> showDeviceInfo()
                cmd.contains("свободная память") || cmd.contains("сколько памяти") || cmd.contains("место на телефоне") -> showStorage()
                cmd.startsWith("скопируй ") || cmd.startsWith("скопировать ") -> copyToClipboard(raw.substringAfter(" "))
                cmd.startsWith("поделись ") || cmd.startsWith("поделиться ") -> shareText(raw.substringAfter(" "))
                cmd.startsWith("позвони ") || cmd.startsWith("набери номер ") -> openDialer(raw.substringAfter(" "))
                cmd.startsWith("напиши смс ") || cmd.startsWith("отправь смс ") -> composeSms(raw.substringAfter("смс").trim())
                cmd.startsWith("маршрут до ") || cmd.startsWith("построй маршрут до ") || cmd.startsWith("найди адрес ") -> openMap(raw.substringAfter("до ").takeIf { cmd.contains("до ") } ?: raw.substringAfter("адрес "))
                cmd.contains("максимальная громкость") || cmd.contains("громкость на максимум") -> setMediaVolume(100)
                cmd.contains("минимальная громкость") || cmd.contains("громкость на минимум") || cmd.contains("выключи звук мультимедиа") -> setMediaVolume(0)
                cmd.contains("громкость") && Regex("\\d{1,3}").containsMatchIn(cmd) -> setMediaVolume(Regex("\\d{1,3}").find(cmd)?.value?.toIntOrNull() ?: 50)
                cmd.contains("громче") || cmd.contains("увеличь громкость") -> changeVolume(true)
                cmd.contains("тише") || cmd.contains("уменьши громкость") -> changeVolume(false)
                cmd.contains("следующий трек") || cmd.contains("следующая песня") -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                cmd.contains("предыдущий трек") || cmd.contains("предыдущая песня") -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                cmd.contains("спасибо") || cmd.contains("благодар") -> respond("Пожалуйста. Чем ещё помочь?")
                isSimpleMath(cmd) -> respond("Ответ: " + evaluateSimpleMath(cmd))
                cmd == "пауза" || cmd.contains("поставь музыку на паузу") -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
                cmd.contains("продолжи музыку") || cmd.contains("продолжить воспроизведение") -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
                cmd == "музыка" || cmd == "включи музыку" || cmd == "открой музыку" || cmd == "запусти музыку" -> openMusicSmart()
                cmd.contains("vk музыка") || cmd.contains("вк музыка") -> openVkMusicExplicit()
                cmd.contains("музыку без интернета") || cmd.contains("локальную музыку") || cmd.contains("музыку на телефоне") -> openLocalMusicPlayer()
                cmd.contains("проводник") || cmd.contains("файлы") || cmd.contains("найди файл") -> openFiles()
                cmd.contains("разрешения jarvis") || cmd.contains("разрешения приложения") -> openAppSettings()
                cmd.contains("звук") || cmd.contains("настройки звука") -> openSettings(android.provider.Settings.ACTION_SOUND_SETTINGS, "Открываю настройки звука.")
                cmd.contains("уведомлен") -> openSettings(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS, "Открываю настройки уведомлений.")
                cmd.contains("яркость") -> openSettings(android.provider.Settings.ACTION_DISPLAY_SETTINGS, "Открываю настройки экрана.")
                cmd.contains("местоположен") || cmd.contains("геолокац") -> openSettings(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Открываю настройки местоположения.")
                cmd.contains("спец возможности") || cmd.contains("специальные возможности") -> openSettings(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, "Открываю специальные возможности Android.")
                cmd.contains("память телефона") || cmd.contains("хранилище") -> openSettings(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Открываю настройки хранилища.")
                cmd.contains("не беспокоить") -> openSettings(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS, "Открываю настройки режима Не беспокоить.")
                cmd.contains("проверь vpn") || cmd.contains("статус vpn") || cmd.contains("работает vpn") || cmd.contains("подключен vpn") || cmd.contains("подключён vpn") -> checkVpn()
                cmd.contains("есть ли интернет") || cmd.contains("статус сети") -> showNetwork()
                cmd in listOf("помощь", "что ты умеешь", "команды") -> respond(helpText())
                cmd.contains("следующий vpn") || cmd.contains("следующий впн") || cmd.contains("другой vpn") || cmd.contains("другой впн") -> nextVpn()
                cmd.contains("wi-fi") || cmd.contains("wifi") || cmd.contains("вайфай") || cmd.contains("вай фай") -> openSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Открываю настройки Wi-Fi.")
                cmd.contains("bluetooth") || cmd.contains("блютуз") -> openSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Открываю настройки Bluetooth.")
                cmd.contains("настройк") -> openSettings(android.provider.Settings.ACTION_SETTINGS, "Открываю настройки.")
                cmd.contains("который час") || cmd == "время" || cmd.contains("сколько времени") -> respond("Сейчас ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}.")
                cmd.contains("какое сегодня число") || cmd.contains("дата") -> respond("Сегодня ${SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())}.")
                (cmd.contains("вк видео") || cmd.contains("vk видео") || cmd.contains("видео вк")) -> {
                    val query = extractMediaQuery(cmd, listOf("вк видео", "vk видео", "видео вк"))
                    openVkVideoSearch(query)
                }
                (cmd.contains("ютуб") || cmd.contains("youtube")) && (cmd.contains("канал") || cmd.contains("видео") || cmd.contains("ролик")) -> {
                    val query = extractMediaQuery(cmd, listOf("ютуб", "youtube", "канал", "видео", "ролик"))
                    openYoutubeSearch(query)
                }
                cmd.startsWith("найди канал ") || cmd.startsWith("открой канал ") || cmd.startsWith("найди видео ") || cmd.startsWith("открой видео ") -> {
                    val query = if (cmd.contains("канал")) cmd.substringAfter("канал").trim() else cmd.substringAfter("видео").trim()
                    openYoutubeSearch(query)
                }
                cmd.startsWith("открой сайт ") || cmd.startsWith("открой страницу ") || cmd.startsWith("перейди на сайт ") || cmd.startsWith("зайди на сайт ") -> {
                    val target = when {
                        cmd.contains("сайт") -> cmd.substringAfter("сайт").trim()
                        cmd.contains("страницу") -> cmd.substringAfter("страницу").trim()
                        else -> ""
                    }
                    openWebsiteOrSearch(target)
                }
                cmd.contains("найди") || cmd.contains("поищи") || cmd.contains("загугли") || cmd.contains("найди в интернете") -> {
                    val query = cmd.replace(Regex("^(найди в интернете|найди|поищи|загугли)\\s*"), "")
                    openWeb(query.ifBlank { raw })
                }
                (cmd.startsWith("открой ") || cmd.startsWith("запусти ") || cmd.startsWith("включи ")) && !cmd.contains("музык") -> {
                    val target = cmd.substringAfter(" ").trim()
                    val pageMarker = Regex("\\s+(?:на странице|страницу|канал|видео|ролик)\\s+").find(target)
                    val appTarget = if (pageMarker != null) target.substring(0, pageMarker.range.first).trim() else target
                    val pageQuery = if (pageMarker != null) target.substring(pageMarker.range.last + 1).trim() else ""
                    val app = findApp(appTarget)
                    if (pageQuery.isNotBlank() && normalizeAppQuery(appTarget) == "youtube") {
                        openYoutubeSearch(pageQuery)
                    }
                    else if (app != null && pageQuery.isNotBlank() && isYouTubeApp(app)) {
                        openYoutubeSearch(pageQuery)
                    }
                    else if (app != null) { launchApp(app.packageName); respond("Запускаю ${app.label}.") }
                    else if (target.contains("браузер") || target.contains("интернет")) openWeb("")
                    else if (target.contains("wi-fi") || target.contains("wifi") || target.contains("вайфай")) openSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Открываю настройки Wi-Fi. Android сам управляет переключателем.")
                    else if (target.contains("bluetooth") || target.contains("блютуз")) openSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Открываю настройки Bluetooth. Android сам управляет переключателем.")
                    else if (target.contains("калькулятор")) openCategoryApp(listOf("калькулятор", "calculator"))
                    else if (target.contains("камер")) openSettings("android.media.action.IMAGE_CAPTURE", "Открываю камеру.")
                    else if (target.contains("музык")) openCategoryApp(listOf("музык", "music", "player"))
                    else if (target.contains("галере") || target.contains("фото")) openCategoryApp(listOf("галере", "фото", "photos", "gallery"))
                    else if (target.contains("карт") || target.contains("навиг")) openCategoryApp(listOf("карты", "maps", "навига"))
                    else if (pageQuery.isNotBlank()) openWeb("$appTarget $pageQuery")
                    else {
                        val close = findApp(appTarget)
                        if (close != null) { launchApp(close.packageName); respond("Нашёл похожее приложение: ${close.label}. Открываю.") }
                        else { respond("Не нашёл приложение «" + appTarget + "». Если это сайт, скажи «открой сайт " + appTarget + "», а для поиска — «найди " + appTarget + "».") }
                    }
                }
                cmd.contains("музык") || cmd.contains("music") -> openCategoryApp(listOf("музык", "music", "player"))
                cmd.contains("галере") || cmd.contains("фото") || cmd.contains("photos") -> openCategoryApp(listOf("галере", "фото", "photos", "gallery"))
                cmd.contains("карты") || cmd.contains("навиг") || cmd.contains("maps") -> openCategoryApp(listOf("карты", "maps", "навига"))
                cmd.contains("интернет") -> openWeb("")
                else -> {
                    val candidate = findApp(cmd)
                    if (candidate != null) { launchApp(candidate.packageName); respond("Похоже, ты имел в виду ${candidate.label}. Открываю.") }
                    else {
                        respond("Не распознал команду. Могу искать в интернете, если ты явно скажешь «найди» или «поищи».")
                    }
                }
            }
        } catch (e: Exception) {
            respond("Не удалось выполнить действие: ${e.localizedMessage ?: "ошибка Android"}.")
        } finally {
            commandStack.remove(cmd)
        }
    }


    private fun teachCommand(raw: String) {
        val body = raw.substringAfter("научи команду").trim()
        val parts = body.split("=", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            respond("Формат: научи команду открыть видео = открой YouTube")
            return
        }
        val phrase = parts[0].trim().lowercase(Locale.getDefault())
        val action = parts[1].trim()
        if (phrase.length < 2 || action.length < 2 || phrase == action.lowercase()) {
            respond("Команда слишком короткая или ссылается сама на себя.")
            return
        }
        prefs.edit().putString("alias_" + phrase, action).apply()
        respond("Запомнил команду «$phrase». Доступны только встроенные действия Jarvis.")
    }

    private fun showAliases() {
        val aliases = prefs.all.filterKeys { it.startsWith("alias_") }.toSortedMap()
        if (aliases.isEmpty()) { respond("Сохранённых пользовательских команд пока нет."); return }
        val lines = aliases.map { (key, value) -> "• ${key.removePrefix("alias_")} = $value" }
        output.text = "МОИ КОМАНДЫ\n" + lines.joinToString("\n")
        say("Сохранено команд: ${aliases.size}. Список показан на экране.")
    }

    private fun deleteAlias(raw: String) {
        val phrase = raw.substringAfter("удали команду").trim().lowercase(Locale.getDefault())
        if (phrase.isBlank()) { respond("Укажи фразу команды, которую нужно удалить."); return }
        if (!prefs.contains("alias_" + phrase)) { respond("Такой сохранённой команды нет."); return }
        prefs.edit().remove("alias_" + phrase).apply()
        respond("Удалил сохранённую команду «$phrase».")
    }

    private fun confirmClearNotes() {
        AlertDialog.Builder(this)
            .setTitle("Удалить все заметки?")
            .setMessage("Это удалит только локальные заметки Jarvis. Отменить действие нельзя.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                val editor = prefs.edit()
                prefs.all.keys.filter { it.startsWith("note_") }.forEach { editor.remove(it) }
                editor.apply()
                respond("Все локальные заметки удалены.")
            }
            .show()
    }

    private fun saveNote(raw: String) {
        val note = raw.substringAfter("запомни", raw.substringAfter("сохрани заметку", "")).trim()
        if (note.isBlank()) { respond("Скажи: запомни купить тетрадь."); return }
        prefs.edit().putString("note_" + System.currentTimeMillis(), note).apply()
        respond("Сохранил заметку на этом телефоне.")
    }

    private fun showNotes() {
        val notes = prefs.all.filterKeys { it.startsWith("note_") }.toSortedMap()
        if (notes.isEmpty()) { respond("Сохранённых заметок пока нет."); return }
        val text = notes.values.joinToString("\n") { it.toString() }
        output.text = "ТВОИ ЗАМЕТКИ\n$text"
        say("У тебя ${notes.size} заметок. Они показаны на экране.")
    }

    private fun setTimer(raw: String) {
        val match = Regex("(\\d+)\\s*(секунд|сек|минут|мин|час|часа|часов)").find(raw.lowercase())
        if (match == null) { respond("Укажи длительность, например: поставь таймер на 5 минут."); return }
        val amount = match.groupValues[1].toIntOrNull() ?: 0
        val unit = match.groupValues[2]
        val secondsLong = when {
            unit.startsWith("сек") -> amount.toLong()
            unit.startsWith("мин") -> amount.toLong() * 60L
            else -> amount.toLong() * 3600L
        }
        if (secondsLong !in 1L..86400L) { respond("Таймер должен быть от одной секунды до 24 часов."); return }
        val seconds = secondsLong.toInt()
        try {
            startActivity(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds).putExtra(AlarmClock.EXTRA_SKIP_UI, false))
            respond("Передал таймер системным часам. Проверь и запусти его.")
        } catch (_: Exception) { respond("На телефоне не найдено приложение часов с поддержкой таймеров.") }
    }

    private fun openAlarm(raw: String = "") {
        try {
            val match = Regex("(?:на\\s*)?(\\d{1,2})[:.](\\d{2})").find(raw)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            if (match != null) {
                val hour = match.groupValues[1].toInt()
                val minute = match.groupValues[2].toInt()
                if (hour !in 0..23 || minute !in 0..59) {
                    respond("Время будильника указано неверно. Используй формат 07:30.")
                    return
                }
                intent.putExtra(AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
                startActivity(intent)
                respond("Передал будильник на $hour:${minute.toString().padStart(2, '0')} системным часам. Проверь и сохрани.")
            } else {
                startActivity(intent)
                respond("Открываю создание будильника. Назови время, например: будильник на 07:30.")
            }
        } catch (_: Exception) { respond("Не удалось открыть приложение часов.") }
    }

    private fun openCalendar() {
        try { startActivity(Intent(Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)); respond("Открываю создание события в календаре. Проверь дату и сохрани его.") }
        catch (_: Exception) { respond("Не найдено приложение календаря.") }
    }

    private fun openFiles() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 42)
            respond("Открыл системный выбор файлов.")
        } catch (_: Exception) { respond("Системный выбор файлов недоступен.") }
    }

    private fun showDeviceInfo() {
        respond("Устройство: ${Build.MANUFACTURER} ${Build.MODEL}. Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).")
    }

    private fun showStorage() {
        try {
            val stats = StatFs(filesDir.absolutePath)
            val freeMb = stats.availableBytes / (1024L * 1024L)
            val totalMb = stats.totalBytes / (1024L * 1024L)
            respond("Во внутреннем разделе данных доступно примерно $freeMb МБ из $totalMb МБ. Это не полный отчёт обо всех разделах телефона.")
        } catch (_: Exception) {
            respond("Не удалось прочитать сведения о свободном месте.")
        }
    }

    private fun showBattery() {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val rawLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val level = if (rawLevel in 0..100) rawLevel.toString() else "неизвестно"
        val status = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        respond("Заряд батареи: $level" + (if (level == "неизвестно") "" else " процентов") + ". " + if (plugged != 0) "Телефон подключён к питанию." else "Телефон работает от аккумулятора.")
    }

    private fun changeVolume(up: Boolean) {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        respond(if (up) "Увеличиваю громкость мультимедиа." else "Уменьшаю громкость мультимедиа.")
    }

    private fun setMediaVolume(percent: Int) {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val safePercent = percent.coerceIn(0, 100)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * safePercent / 100f).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        respond("Громкость мультимедиа установлена примерно на $safePercent процентов.")
    }

    private fun sendMediaKey(keyCode: Int) {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        respond("Передал команду управления воспроизведением активному медиаплееру.")
    }

    private fun toggleTorch(turnOff: Boolean) {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 77)
            respond("Android запросил разрешение камеры для фонарика. Если разрешишь, фонарик включится.")
            return
        }
        try {
            val camera = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            if (id == null) { respond("На устройстве не обнаружена вспышка."); return }
            camera.setTorchMode(id, !turnOff)
            respond(if (turnOff) "Выключил фонарик." else "Включил фонарик.")
        } catch (_: SecurityException) {
            respond("Android не предоставил доступ к вспышке. Проверь разрешение камеры для Jarvis.")
        } catch (e: Exception) {
            respond("Не удалось переключить фонарик: ${e.localizedMessage ?: "ошибка устройства"}.")
        }
    }

    private fun checkVpn() {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
        val caps = network?.let { manager.getNetworkCapabilities(it) }
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
            respond("Android сообщает, что VPN-туннель активен. Доступность конкретных сайтов этим не гарантируется.")
        } else {
            respond("Android не показывает активный VPN-туннель. Пробую следующий установленный клиент, если он найден.")
            nextVpn()
        }
    }

    private fun showNetwork() {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
        val caps = network?.let { manager.getNetworkCapabilities(it) }
        if (caps == null) { respond("Сейчас нет активного сетевого подключения."); return }
        val vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val mobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        respond("Сеть подключена. Тип: " + when { vpn -> "VPN-туннель"; wifi -> "Wi-Fi"; mobile -> "мобильная сеть"; else -> "другая сеть" } + ".")
    }

    private fun findVpnCandidates(): List<AppEntry> {
        val vpnWords = listOf(
            "vpn", "впн", "wireguard", "openvpn", "tunnel", "прокси", "proton",
            "warp", "cloudflare", "psiphon", "amnezia", "outline", "adguard",
            "windscribe", "hide.me", "surfshark", "nordvpn", "expressvpn",
            "riseup", "v2ray", "sing-box", "nekobox", "hiddify", "shadowsocks",
            "karing", "v2box", "clash"
        )
        return installedApps.filter { app ->
            val value = (app.label + " " + app.packageName).lowercase(Locale.ROOT)
            vpnWords.any { value.contains(it) }
        }.distinctBy { it.packageName }.sortedBy { app ->
            val value = (app.label + " " + app.packageName).lowercase(Locale.ROOT)
            when {
                value.contains("wireguard") -> 0
                value.contains("proton") -> 1
                value.contains("amnezia") -> 2
                value.contains("openvpn") -> 3
                value.contains("warp") || value.contains("1.1.1.1") -> 4
                value.contains("windscribe") -> 5
                else -> 6
            }
        }
    }

    private fun openAiRandom() {
        val apps = installedApps.filter {
            val v = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
            v.contains("chatgpt") || v.contains("openai") || v.contains("claude") || v.contains("anthropic")
        }.distinctBy { it.packageName }
        if (apps.isEmpty()) { respond("Не нашёл ChatGPT или Claude. Google вместо ИИ открывать не буду."); return }
        val app = apps.random()
        try { launchApp(app.packageName); respond("Открываю ИИ: " + app.label + ".") }
        catch (_: Exception) { respond("Не удалось открыть " + app.label + ".") }
    }

    private fun openChatGpt() {
        val app = installedApps.firstOrNull {
            val v = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
            v.contains("chatgpt") || v.contains("openai") || v.contains("чатджпт")
        }
        if (app == null) respond("ChatGPT не найден среди установленных приложений.")
        else { launchApp(app.packageName); respond("Открываю ChatGPT.") }
    }

    private fun openClaude() {
        val app = installedApps.firstOrNull {
            val v = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
            v.contains("claude") || v.contains("anthropic") || v.contains("клод")
        }
        if (app == null) respond("Claude не найден среди установленных приложений.")
        else { launchApp(app.packageName); respond("Открываю Claude.") }
    }

    private fun handleConversation(cmd: String): Boolean {
        return when {
            cmd in listOf("привет","здравствуй","здравствуй jarvis","добрый день","добрый вечер","доброе утро") -> { respond("Привет. Я здесь. Говори."); true }
            cmd.contains("как дела") || cmd.contains("как ты") -> { respond("Работаю нормально. Готов помочь."); true }
            cmd.contains("кто ты") || cmd.contains("что ты умеешь") -> { respond("Я JARVIS. Могу открывать приложения, управлять музыкой и настройками, искать информацию и выполнять локальные команды."); true }
            cmd.contains("что ты умеешь без интернета") -> { respond("Без интернета работают локальные команды, приложения, музыка, громкость, фонарик и настройки. Для актуальных знаний нужен интернет."); true }
            cmd.contains("спасибо") || cmd.contains("благодар") -> { respond("Пожалуйста."); true }
            cmd.contains("молодец") || cmd.contains("круто") -> { respond("Принято."); true }
            cmd.contains("повтори ответ") || cmd == "повтори" -> { respond(output.text.toString()); true }
            cmd.contains("не понял") || cmd.contains("ты не понял") -> { respond("Скажи короче: «открой ChatGPT», «включи музыку» или «открой VPN»."); true }
            cmd.contains("расскажи анекдот") -> { respond("Почему программист не любит природу? Там слишком много багов."); true }
            cmd.contains("интересный факт") || cmd.contains("расскажи факт") -> { respond("У осьминога три сердца."); true }
            cmd.startsWith("что такое ") || cmd.startsWith("кто такой ") || cmd.startsWith("кто такая ") ||
                cmd.startsWith("почему ") || cmd.startsWith("зачем ") || cmd.startsWith("как работает ") ||
                cmd.startsWith("объясни ") || cmd.startsWith("расскажи про ") -> {
                respond("Я понял вопрос, но в этой версии нет встроенной языковой модели. Я не буду притворяться, что знаю ответ, и не стану молча отправлять тебя в Google.")
                true
            }
            cmd.startsWith("ответь ") || cmd.startsWith("объясни мне ") -> {
                respond("В этой версии у меня нет встроенной ИИ-модели для свободного разговора. Поэтому я не буду подменять разговор поиском Google.")
                true
            }
            else -> false
        }
    }

    private fun openVpn() {
        vpnCandidates = findVpnCandidates().shuffled()
        vpnIndex = -1
        nextVpn()
    }

    private fun openAdGuardVpn() {
        val installed = packageManager.getLaunchIntentForPackage("com.adguard.vpn") != null
        if (!installed) {
            val candidate = installedApps.firstOrNull {
                val value = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
                value.contains("adguard") && value.contains("vpn")
            }
            if (candidate != null) {
                launchApp(candidate.packageName)
                respond("Открыл AdGuard VPN. Если Android попросит разрешение на VPN-подключение, подтверди его в системном окне.")
            } else {
                respond("AdGuard VPN не найден среди установленных приложений. Я не буду подменять его другим VPN.")
            }
            return
        }
        try {
            launchApp("com.adguard.vpn")
            respond("Открыл AdGuard VPN. Если Android попросит разрешение на VPN-подключение, подтверди его в системном окне, а затем подключись в самом AdGuard VPN.")
        } catch (_: Exception) {
            respond("Не удалось открыть AdGuard VPN. Я не буду подменять его другим VPN.")
        }
    }

    private fun nextVpn() {
        if (vpnCandidates.isEmpty()) vpnCandidates = findVpnCandidates().shuffled()
        if (vpnCandidates.isEmpty()) {
            respond("Не нашёл известный VPN-клиент среди запускаемых приложений. Открываю системные настройки VPN.")
            openSettings("android.settings.VPN_SETTINGS", "Выбери VPN-клиент в настройках Android.")
            return
        }
        val nextIndex = vpnIndex + 1
        if (nextIndex >= vpnCandidates.size) {
            respond("Других найденных VPN-клиентов нет. Я не могу проверить соединение автоматически без системного разрешения VPN.")
            return
        }
        vpnIndex = nextIndex
        val candidate = vpnCandidates[vpnIndex]
        try {
            launchApp(candidate.packageName)
            respond("Запустил вариант ${vpnIndex + 1} из ${vpnCandidates.size}: ${candidate.label}. Подтверди подключение в самом приложении. Если оно не подключится, скажи «следующий VPN». Я не вижу надёжный статус соединения без отдельного системного VPN-сервиса.")
        } catch (_: Exception) {
            respond("${candidate.label} не удалось открыть. Попробуй команду «следующий VPN» для следующего найденного приложения.")
        }
    }


    private fun hasInternetConnection(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun findVkMusicApp(): AppEntry? {
        return installedApps.firstOrNull {
            val value = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
            value.contains("vk") && (value.contains("music") || value.contains("музык"))
        }
    }

    private fun openVkMusicExplicit() {
        val app = findVkMusicApp()
        if (app != null) {
            launchApp(app.packageName)
            respond("Открываю VK Музыку.")
        } else {
            respond("Приложение VK Музыка не найдено.")
        }
    }

    private fun openMusicSmart() {
        if (hasInternetConnection()) {
            val app = findVkMusicApp()
            if (app != null) {
                launchApp(app.packageName)
                respond("Открываю VK Музыку.")
            } else {
                respond("Интернет есть, но приложение VK Музыка не найдено. Установи его или скажи «локальная музыка».")
            }
        } else {
            openLocalMusicPlayer()
        }
    }

    private fun openLocalMusicPlayer() {
        val app = installedApps.firstOrNull {
            val value = (it.label + " " + it.packageName).lowercase(Locale.ROOT)
            (value.contains("музык") || value.contains("music") || value.contains("player") || value.contains("плеер")) &&
                !value.contains("video") && !value.contains("видео")
        }
        if (app != null) {
            launchApp(app.packageName)
            respond("Интернета нет. Открываю локальный музыкальный плеер: ${app.label}.")
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply { type = "audio/*" })
            respond("Открываю установленное приложение для музыки.")
        } catch (_: Exception) {
            respond("Интернета нет, а музыкальный плеер не найден.")
        }
    }

    private fun openCategoryApp(keywords: List<String>) {
        val app = installedApps.firstOrNull { entry ->
            val label = entry.label.lowercase(Locale.getDefault())
            keywords.any { label.contains(it) }
        }
        if (app == null) respond("Не нашёл подходящее приложение в установленном списке.")
        else {
            launchApp(app.packageName)
            respond("Открываю ${app.label}.")
        }
    }

    private fun normalizeAppQuery(value: String): String {
        var cleaned = value.lowercase(Locale.ROOT)
            .replace(Regex("^(приложение|программу|программа|app)\\s+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
        val aliases = mapOf(
            "ютуб" to "youtube", "ютуп" to "youtube", "ютюб" to "youtube", "ютубе" to "youtube",
            "yutub" to "youtube", "yutube" to "youtube", "youtub" to "youtube",
            "телега" to "telegram", "телеграм" to "telegram", "телеграмм" to "telegram",
            "ватсап" to "whatsapp", "вотсап" to "whatsapp", "вацап" to "whatsapp",
            "вконтакте" to "vk", "вк" to "vk", "тикток" to "tiktok",
            "инста" to "instagram", "инстаграм" to "instagram",
            "дискорд" to "discord", "стим" to "steam", "эпик" to "epicgames",
            "яндекс" to "yandex", "хром" to "chrome",
            "чатджпт" to "chatgpt", "чатжпт" to "chatgpt", "джпт" to "chatgpt", "жпт" to "chatgpt",
            "клод" to "claude", "антропик" to "claude",
            "ии" to "ai", "айи" to "ai", "ай" to "ai"
        )
        cleaned = aliases[cleaned] ?: cleaned
        return cleaned
    }

    private fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

    private fun findApp(query: String): AppEntry? {
        val q = normalizeAppQuery(query)
        if (q.length < 2) return null

        val aliases = mapOf(
            "ютуб" to listOf("youtube", "ютуб"),
            "youtube" to listOf("youtube", "ютуб"),
            "вк" to listOf("vk", "вконтакте", "вк"),
            "вконтакте" to listOf("vk", "вконтакте", "вк"),
            "браузер" to listOf("browser", "браузер", "chrome", "firefox", "яндекс"),
            "интернет" to listOf("browser", "chrome", "firefox", "яндекс"),
            "музыка" to listOf("music", "музык", "плеер", "player"),
            "видео" to listOf("video", "видео", "player", "плеер"),
            "галерея" to listOf("gallery", "галере", "фото", "photos"),
            "фото" to listOf("gallery", "галере", "фото", "photos"),
            "карты" to listOf("maps", "карты", "навига"),
            "навигатор" to listOf("maps", "карты", "навига"),
            "калькулятор" to listOf("calculator", "калькулятор"),
            "файлы" to listOf("files", "файлы", "проводник", "filemanager"),
            "chatgpt" to listOf("chatgpt", "openai", "чат", "джпт"),
            "claude" to listOf("claude", "anthropic", "клод"),
            "ai" to listOf("chatgpt", "claude", "openai", "anthropic", "ии")
        )
        val expanded = (listOf(q) + (aliases[q] ?: emptyList())).distinct()

        val ranked = installedApps.map { app ->
            val label = normalizeAppQuery(app.label)
            val pkg = normalizeAppQuery(app.packageName)
            val pkgShort = normalizeAppQuery(app.packageName.substringAfterLast('.'))
            val candidates = listOf(label, pkgShort, pkg).filter { it.isNotBlank() }

            val score = expanded.maxOfOrNull { term ->
                candidates.maxOfOrNull { candidate ->
                    when {
                        candidate == term -> 140
                        candidate.startsWith(term) -> 115
                        candidate.contains(term) -> 95
                        term.length >= 4 && term.contains(candidate) -> 80
                        else -> {
                            val distance = editDistance(term, candidate)
                            val allowed = when {
                                term.length >= 9 -> 2
                                term.length >= 6 -> 1
                                else -> 0
                            }
                            if (distance <= allowed) 65 - distance * 10 else 0
                        }
                    }
                } ?: 0
            } ?: 0
            app to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        if (ranked.isEmpty()) return null
        if (ranked.size > 1 && ranked[0].second == ranked[1].second) return null
        return ranked.first().first
    }

    private fun launchApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: throw ActivityNotFoundException()
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
            startActivity(intent)
            respond("Открыл настройки Jarvis. Здесь можно посмотреть и изменить выданные разрешения.")
        } catch (_: Exception) {
            respond("Не удалось открыть настройки разрешений Jarvis.")
        }
    }

    private fun openSettings(action: String, message: String) {
        try { startActivity(Intent(action)); respond(message) }
        catch (_: Exception) { respond("Эти настройки недоступны на данной версии Android.") }
    }

    private fun copyToClipboard(value: String) {
        if (value.isBlank()) { respond("Укажи текст после команды «скопируй»."); return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("JARVIS", value))
        respond("Текст скопирован в буфер обмена.")
    }

    private fun shareText(value: String) {
        if (value.isBlank()) { respond("Укажи текст, которым нужно поделиться."); return }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
        }
        startActivity(Intent.createChooser(send, "Поделиться через"))
        respond("Открыл системное меню отправки. Выбери приложение и получателя.")
    }

    private fun openDialer(value: String) {
        val number = value.trim()
        if (number.isBlank()) { respond("Укажи номер телефона."); return }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number.replace(" ", ""), null)))
        respond("Открыл номеронабиратель. Вызов не выполняется автоматически.")
    }

    private fun composeSms(value: String) {
        val parts = Regex("\\s+").split(value.trim(), limit = 2)
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            respond("Формат: напиши смс +79990000000 текст сообщения.")
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", parts[0], null)).apply {
            putExtra("sms_body", parts[1])
        }
        startActivity(intent)
        respond("Открыл приложение сообщений с подготовленным текстом. Само сообщение не отправлено.")
    }

    private fun openMap(destination: String) {
        val place = destination.trim()
        if (place.isBlank()) { respond("Укажи место назначения."); return }
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(place))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            respond("Открываю карту для поиска: $place")
        } catch (_: Exception) {
            openWeb("карта маршрут $place")
        }
    }

    private fun isSimpleMath(cmd: String): Boolean {
        val value = cmd.replace("сколько будет", "").replace("посчитай", "").replace("вычисли", "").replace("реши", "").replace("=", "").replace(",", ".").trim()
        return Regex("""^[-+]?\d+(?:\.\d+)?(?:\s*[+\-*/xх]\s*[-+]?\d+(?:\.\d+)?)+$""").matches(value)
    }

    private fun evaluateSimpleMath(cmd: String): String {
        val value = cmd.replace("сколько будет", "").replace("посчитай", "").replace("вычисли", "").replace("реши", "").replace("=", "").replace(",", ".").replace("x", "*", true).replace("х", "*").trim()
        val tokens = Regex("""[-+*/]|\d+(?:\.\d+)?""").findAll(value).map { it.value }.toList()
        if (tokens.isEmpty()) return "не удалось посчитать"
        var result = tokens[0].toDouble()
        var j = 1
        while (j + 1 < tokens.size) {
            val number = tokens[j + 1].toDouble()
            result = when (tokens[j]) { "+" -> result + number; "-" -> result - number; "*" -> result * number; "/" -> if (number != 0.0) result / number else return "деление на ноль невозможно"; else -> return "не удалось посчитать" }
            j += 2
        }
        return if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.6f", result).trimEnd('0').trimEnd('.')
    }

    private fun openWeb(query: String) {
        val url = if (query.isBlank()) "https://www.google.com/" else "https://www.google.com/search?q=" + Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
            respond(if (query.isBlank()) "Открываю Google." else "Ищу в Google: $query")
        } catch (_: ActivityNotFoundException) {
            respond("На устройстве не найден браузер для открытия Google.")
        }
    }

    private fun openWebsiteOrSearch(rawTarget: String) {
        val target = rawTarget.trim().trimEnd('.', ',', '!', '?')
        if (target.isBlank()) { respond("Укажи название или адрес сайта."); return }
        val siteAliases = mapOf(
            "ютуб" to "https://www.youtube.com/",
            "youtube" to "https://www.youtube.com/",
            "гугл" to "https://www.google.com/",
            "google" to "https://www.google.com/",
            "вк" to "https://vk.com/",
            "вконтакте" to "https://vk.com/",
            "яндекс" to "https://ya.ru/",
            "yandex" to "https://ya.ru/",
            "википедия" to "https://ru.wikipedia.org/",
            "wikipedia" to "https://www.wikipedia.org/",
            "гитхаб" to "https://github.com/",
            "github" to "https://github.com/",
            "почта гугл" to "https://mail.google.com/",
            "gmail" to "https://mail.google.com/"
        )
        val aliasUrl = siteAliases[target.lowercase(Locale.ROOT)]
        val hasDomain = Regex("(?i)^(https?://)?([a-z0-9-]+\\.)+[a-z]{2,}(/.*)?$").matches(target)
        if (aliasUrl == null && !hasDomain) {
            val directUrl = "https://" + target.removePrefix("https://").removePrefix("http://")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)))
                respond("Пробую открыть сайт напрямую: $target.")
            } catch (_: Exception) {
                respond("Не смог определить адрес сайта «$target». Если хочешь поиск, скажи «найди $target».")
            }
            return
        }
        val url = aliasUrl ?: if (target.startsWith("http://", true) || target.startsWith("https://", true)) target else "https://$target"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            respond("Открываю сайт: $target.")
        } catch (_: Exception) {
            respond("Не удалось открыть сайт напрямую. Если хочешь поиск, скажи «найди $target».")
        }
    }

    private fun extractMediaQuery(cmd: String, markers: List<String>): String {
        var value = cmd
        markers.forEach { marker -> value = value.replace(marker, " ") }
        value = value.replace(Regex("\\b(в|на)\\s+(ютубе|youtube|вк|vk)\\b"), " ")
        value = value.replace(Regex("\\s+"), " ").trim()
        return value
    }

    private fun openVkVideoSearch(query: String) {
        val safeQuery = query.trim()
        val url = if (safeQuery.isBlank()) "https://vk.com/video" else "https://vk.com/video?q=" + Uri.encode(safeQuery)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            respond(if (safeQuery.isBlank()) "Открываю VK Видео." else "Открываю VK Видео по запросу: $safeQuery.")
        } catch (_: Exception) {
            respond("Не удалось открыть VK Видео.")
        }
    }

    private fun openYoutubeSearch(query: String) {
        val safeQuery = query.trim()
        if (safeQuery.isBlank()) {
            try {
                startActivity(packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/")))
                respond("Открываю YouTube.")
            } catch (_: Exception) { openWeb("YouTube") }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(safeQuery)))
        intent.setPackage("com.google.android.youtube")
        try {
            startActivity(intent)
            respond("Открываю поиск YouTube: $safeQuery.")
        } catch (_: Exception) {
            intent.setPackage(null)
            try { startActivity(intent); respond("Открываю результаты YouTube для запроса: $safeQuery.") }
            catch (_: Exception) { openWeb("YouTube $safeQuery") }
        }
    }

    private fun isYouTubeApp(app: AppEntry): Boolean {
        val value = (app.label + " " + app.packageName).lowercase(Locale.ROOT)
        return value.contains("youtube") || value.contains("ютуб") || app.packageName == "com.google.android.youtube"
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toggleTorch(false)
        else if (requestCode == 77) respond("Разрешение не выдано. Фонарик не включён.")
        else if (requestCode == 53 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) enableOverlay()
        else if (requestCode == 53) respond("Без уведомления плавающий режим не запущен. Разрешение можно выдать в настройках JARVIS.")
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == Activity.RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { input.setText(spoken); runCommand(spoken) }
        } else if (requestCode == 52) {
            if (Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this)) enableOverlay()
            else respond("Разрешение на показ поверх окон не выдано. Плавающая кнопка не включена.")
        } else if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                val name = uri.lastPathSegment ?: "выбранный файл"
                respond("Файл выбран: $name. Открываю системным приложением.")
                try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, contentResolver.getType(uri) ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                catch (_: Exception) { respond("Файл выбран, но приложения для его открытия нет.") }
            }
        }
    }

    private fun helpText() = "Я умею: запускать установленные приложения; показывать иконки; управлять громкостью и воспроизведением; включать фонарик; проверять заряд, сведения об устройстве, свободное место и тип сети; копировать и отправлять текст через системное меню; открывать номеронабиратель и создавать черновик SMS без автоматической отправки; искать места на карте; открывать Wi-Fi, Bluetooth, VPN, экранные настройки, местоположение, специальные возможности, хранилище, режим Не беспокоить, файлы и календарь; создавать таймеры и будильники через системные часы; сохранять и удалять заметки с подтверждением; просматривать и удалять пользовательские команды; запоминать псевдонимы команд; выполнять простые цепочки действий через слово «потом»; искать в интернете; принимать голосовые команды. Работаю локально без API-ключей. Можно включить плавающую кнопку JARVIS и открыть компактное окно двойным нажатием по ней; для этого нужно разрешение «Поверх других окон». Скажи «выключи плавающее окно», чтобы убрать кнопку. Сложное свободное рассуждение без локальной языковой модели пока недоступно."

    private fun respond(message: String) {
        output.text = message
        say(message)
    }

    private fun say(message: String) {
        if (ttsReady) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun roundedBackground(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun addSpace(size: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(size))) }
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OVERLAY_COMMAND = "com.urchi.jarvis.OVERLAY_COMMAND"
        const val EXTRA_OVERLAY_VOICE = "com.urchi.jarvis.OVERLAY_VOICE"
    }
}
