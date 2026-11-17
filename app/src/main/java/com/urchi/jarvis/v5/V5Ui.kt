package com.urchi.jarvis.v5

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import com.urchi.jarvis.ChatStore

class V5Ui(
    private val context: Context,
    private val onSend: (String) -> Unit,
    private val onSpeak: () -> Unit,
    private val onNewChat: () -> Unit,
    private val onChats: () -> Unit,
    private val onVoiceSettings: () -> Unit,
    private val onPermissions: () -> Unit
) {
    lateinit var root: LinearLayout
    lateinit var messages: LinearLayout
    lateinit var input: EditText

    private val bg = Color.rgb(7, 11, 17)
    private val surface = Color.rgb(15, 22, 31)
    private val surface2 = Color.rgb(20, 29, 40)
    private val line = Color.rgb(39, 52, 67)
    private val accent = Color.rgb(76, 220, 196)
    private val white = Color.rgb(242, 246, 249)
    private val muted = Color.rgb(142, 156, 171)

    fun build(): View {
        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(12), dp(16), dp(8))
            clipToPadding = false
            ViewCompatInsets.install(this)
        }

        root.addView(header(), LinearLayout.LayoutParams(-1, dp(68)))
        root.addView(statusCard(), LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(8) })
        root.addView(sectionTitle("Быстрый доступ"), LinearLayout.LayoutParams(-1, dp(32)).apply { topMargin = dp(10) })
        root.addView(quickActions(), LinearLayout.LayoutParams(-1, dp(58)))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            clipToPadding = false
        }
        messages = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }
        scroll.addView(messages, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(composer(), LinearLayout.LayoutParams(-1, dp(64)).apply { topMargin = dp(6) })
        return root
    }

    private fun header(): View {
        val box = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val avatar = TextView(context).apply {
            text = "J"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = rounded(accent, 18f)
        }
        box.addView(avatar, LinearLayout.LayoutParams(dp(42), dp(42)))
        val title = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        title.addView(TextView(context).apply {
            text = "JARVIS"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        })
        title.addView(TextView(context).apply {
            text = "Личный интеллектуальный помощник"
            textSize = 11f
            setTextColor(muted)
        })
        box.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(smallButton("Доступы", "⌁") { onPermissions() }, LinearLayout.LayoutParams(dp(104), dp(46)))
        return box
    }

    private fun statusCard(): View {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(surface, 16f, line, 1)
        }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(context).apply {
            text = "●  Система готова"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(context).apply {
            text = "V5"
            textSize = 11f
            setTextColor(muted)
        })
        box.addView(top)
        box.addView(TextView(context).apply {
            text = "Язык  •  Знания  •  Память  •  Действия"
            textSize = 11f
            setTextColor(muted)
            setPadding(0, dp(5), 0, 0)
        })
        return box
    }

    private fun sectionTitle(text: String): View = TextView(context).apply {
        this.text = text.uppercase()
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(muted)
        gravity = Gravity.CENTER_VERTICAL
        letterSpacing = .08f
    }

    private fun quickActions(): View {
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(actionChip("＋  Новый чат") { onNewChat() }, weightParams())
        row.addView(actionChip("▣  Чаты") { onChats() }, weightParams())
        row.addView(actionChip("◉  Голос") { onSpeak() }, weightParams())
        row.addView(actionChip("⚙  Голос") { onVoiceSettings() }, weightParams())
        return row
    }

    private fun actionChip(text: String, action: () -> Unit): View = TextView(context).apply {
        this.text = text
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(white)
        background = rounded(surface2, 13f, line, 1)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
        setPadding(dp(3), 0, dp(3), 0)
    }

    private fun composer(): View {
        val shell = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(surface2, 18f, line, 1)
        }
        input = EditText(context).apply {
            hint = "Напишите команду или вопрос…"
            textSize = 14f
            setTextColor(white)
            setHintTextColor(muted)
            maxLines = 3
            minLines = 1
            background = null
            setPadding(dp(8), 0, dp(6), 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    send()
                    true
                } else false
            }
        }
        shell.addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        shell.addView(TextView(context).apply {
            text = "➤"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = rounded(accent, 17f)
            isClickable = true
            setOnClickListener { send() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        return shell
    }

    private fun smallButton(label: String, icon: String, action: () -> Unit): View =
        TextView(context).apply {
            text = "$icon  $label"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(surface2, 13f, line, 1)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    fun addMessage(role: String, text: String) {
        val mine = role == "user"
        val cardView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(if (mine) Color.rgb(19, 45, 48) else surface, 15f, if (mine) Color.rgb(40, 96, 91) else line, 1)
            contentDescription = if (mine) "Сообщение пользователя" else "Сообщение JARVIS"
        }
        cardView.addView(TextView(context).apply {
            this.text = if (mine) "ТЫ" else "JARVIS"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (mine) accent else muted)
        })
        cardView.addView(TextView(context).apply {
            this.text = text
            textSize = 15f
            setTextColor(white)
            setPadding(0, dp(4), 0, 0)
        })
        val params = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(if (mine) dp(38) else 0, dp(6), if (mine) 0 else dp(38), dp(6))
        }
        messages.addView(cardView, params)
    }

    fun clearMessages() { messages.removeAllViews() }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isNotBlank()) {
            input.text.clear()
            onSend(text)
        }
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, -1, 1f).apply {
        marginStart = dp(3)
        marginEnd = dp(3)
    }

    private fun rounded(fill: Int, radius: Float, stroke: Int? = null, width: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null && width > 0) setStroke(dp(width), stroke)
        }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    private object ViewCompatInsets {
        fun install(view: View) {
            view.setOnApplyWindowInsetsListener { v, insets ->
                val bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, maxOf(v.paddingBottom, bottom))
                insets
            }
            view.requestApplyInsets()
        }
    }
}
