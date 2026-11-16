package com.urchi.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.min

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var lastTapAt = 0L
    private val panelColor = Color.rgb(16, 27, 38)
    private val accent = Color.rgb(83, 215, 194)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши JARVIS показываться поверх других окон", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun showBubble() {
        val view = OrbBubbleView(this).apply {
            contentDescription = "JARVIS. Дважды нажми, чтобы открыть быстрый запрос"
            elevation = dp(10).toFloat()
        }
        val params = WindowManager.LayoutParams(
            dp(58), dp(58), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(220)
        }
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > dp(7) || abs(dy) > dp(7)) moved = true
                    if (moved) {
                        params.x = (startX - dx).coerceAtLeast(0)
                        params.y = (startY + dy).coerceAtLeast(0)
                        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (lastTapAt > 0L && now > lastTapAt && now - lastTapAt <= 380L) {
                            lastTapAt = 0L
                            showPanel()
                        } else {
                            lastTapAt = now
                        }
                    }
                    true
                }
                else -> true
            }
        }
        try {
            windowManager.addView(view, params)
            bubble = view
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось показать плавающее окно JARVIS", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun showPanel() {
        if (panel != null) return
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = rounded(panelColor, dp(20))
            elevation = dp(14).toFloat()
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "JARVIS  •  БЫСТРЫЙ ЗАПРОС"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(40), 1f))
        val close = TextView(this).apply {
            text = "×"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { hidePanel() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(40), dp(40)))
        card.addView(header)

        val request = EditText(this).apply {
            hint = "Напиши запрос JARVIS..."
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            isSingleLine = false
            minLines = 1
            maxLines = 3
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(29, 43, 56), dp(12))
        }
        card.addView(request, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(8)
            bottomMargin = dp(10)
        })

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val send = actionButton("ОТПРАВИТЬ", true) {
            val command = request.text.toString().trim()
            if (command.isBlank()) {
                request.error = "Введи запрос"
            } else {
                hidePanel()
                val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_OVERLAY_COMMAND, command)
                }
                startActivity(intent)
            }
        }
        buttons.addView(send, LinearLayout.LayoutParams(0, dp(46), 1f))
        val voice = actionButton("ГОЛОС", false) {
            hidePanel()
            val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OVERLAY_VOICE, true)
            }
            startActivity(intent)
        }
        buttons.addView(voice, LinearLayout.LayoutParams(dp(92), dp(46)).apply { leftMargin = dp(8) })
        card.addView(buttons)

        val screenWidth = resources.displayMetrics.widthPixels
        val params = WindowManager.LayoutParams(
            min(screenWidth - dp(28), dp(380)), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windowManager.addView(card, params)
            panel = card
            request.requestFocus()
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть окно JARVIS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hidePanel() {
        panel?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun actionButton(label: String, primary: Boolean, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(if (primary) Color.rgb(5, 24, 27) else Color.WHITE)
            background = rounded(if (primary) accent else Color.rgb(38, 55, 69), dp(12))
            setOnClickListener { action() }
        }
    }

    private class OrbBubbleView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = min(width, height) * .43f
            paint.shader = RadialGradient(
                cx - r * .30f, cy - r * .36f, r * 1.3f,
                intArrayOf(Color.WHITE, Color.rgb(83, 215, 194), Color.rgb(23, 70, 86), Color.rgb(8, 18, 28)),
                floatArrayOf(0f, .18f, .62f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * .07f
            paint.color = Color.rgb(83, 215, 194)
            canvas.drawArc(cx-r*1.08f, cy-r*1.08f, cx+r*1.08f, cy+r*1.08f, phase, 210f, false, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(cx-r*.34f, cy-r*.38f, r*.11f, paint)
            phase = (phase + 2f) % 360f
            postInvalidateDelayed(32L)
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS — плавающее окно", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Уведомление нужно, пока плавающая кнопка JARVIS активна"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val open = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("JARVIS активен")
                .setContentText("Дважды нажми плавающую кнопку, чтобы задать запрос")
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("JARVIS активен")
                .setContentText("Плавающая кнопка готова")
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {
        hidePanel()
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        bubble = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isRunning = false\n        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "jarvis_overlay"
        private const val NOTIFICATION_ID = 7314
    }
}