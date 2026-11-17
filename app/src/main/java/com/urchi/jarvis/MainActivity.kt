package com.urchi.jarvis

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.urchi.jarvis.v5.V5ActionExecutor
import com.urchi.jarvis.v5.V5AssistantEngine
import com.urchi.jarvis.v5.V5Ui
import com.urchi.jarvis.v5.V5Planner
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var ui: V5Ui
    private lateinit var chats: ChatStore
    private lateinit var engine: V5AssistantEngine
    private lateinit var actions: V5ActionExecutor
    private val planner = V5Planner()
    private lateinit var tts: TextToSpeech
    private var activeChatId: Long = 0L
    private var ttsReady = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        chats = ChatStore(this)
        activeChatId = chats.getOrCreate().id
        engine = V5AssistantEngine(this)
        actions = V5ActionExecutor(this)
        tts = TextToSpeech(this, this)
        ui = V5Ui(
            context = this,
            onSend = ::process,
            onSpeak = ::listen,
            onNewChat = ::newChat,
            onPermissions = { startActivity(Intent(this, PermissionCenterActivity::class.java)) }
        )
        setContentView(ui.build())
        restoreChat()
    }

    private fun restoreChat() {
        val chat = chats.list().firstOrNull { it.id == activeChatId } ?: return
        chat.messages.takeLast(80).forEach { ui.addMessage(it.role, it.text) }
    }

    private fun newChat() {
        activeChatId = chats.create().id
        ui.clearMessages()
        ui.addMessage("assistant", "Новый чат создан.")
    }

    private fun process(text: String) {
        ui.addMessage("user", text)
        chats.add(activeChatId, "user", text)

        val segments = planner.split(text)
        val results = mutableListOf<String>()

        for (segment in segments) {
            val reply = engine.answer(segment)
            var response = reply.text
            if (reply.command != null) {
                val action = actions.execute(reply.command)
                if (action.message.isNotBlank()) response = action.message
            }
            if (response.isNotBlank()) results += response
        }

        val response = results.joinToString("\n")
            .ifBlank { "Команду не удалось распознать." }
        ui.addMessage("assistant", response)
        chats.add(activeChatId, "assistant", response)
        speak(response)
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Слушаю…")
        }
        try { startActivityForResult(intent, REQUEST_SPEECH) } catch (_: Exception) {
            ui.addMessage("assistant", "На устройстве нет доступного распознавания речи.")
        }
    }

    @Deprecated("Android compatibility callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SPEECH || data == null) return
        val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        process(text)
    }

    private fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts.stop()
        tts.language = Locale("ru", "RU")
        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)
        tts.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis-response")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale("ru", "RU")
            tts.setSpeechRate(1.0f)
            tts.setPitch(1.0f)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    companion object { private const val REQUEST_SPEECH = 7101 }
}
