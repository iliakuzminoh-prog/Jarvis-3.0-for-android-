package com.urchi.jarvis

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.SeekBar
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.urchi.jarvis.v5.V5ActionExecutor
import com.urchi.jarvis.v5.V5AssistantEngine
import com.urchi.jarvis.v5.V5Ui
import com.urchi.jarvis.v5.V5Planner
import java.util.Locale

class MainActivity:AppCompatActivity(),TextToSpeech.OnInitListener {
    private lateinit var ui:V5Ui
    private lateinit var chats:ChatStore
    private lateinit var engine:V5AssistantEngine
    private lateinit var actions:V5ActionExecutor
    private val planner=V5Planner()
    private lateinit var tts:TextToSpeech
    private var activeChatId=0L
    private var ttsReady=false
    private var speechRate=1.0f
    private var speechPitch=1.0f

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        chats=ChatStore(this)
        activeChatId=chats.getOrCreate().id
        engine=V5AssistantEngine(this)
        actions=V5ActionExecutor(this)
        tts=TextToSpeech(this,this)
        ui=V5Ui(this,::process,::listen,::newChat,::showChats,::showVoiceSettings){
            startActivity(Intent(this,PermissionCenterActivity::class.java))
        }
        setContentView(ui.build())
        restoreChat()
    }
    private fun restoreChat() {
        val chat=chats.list().firstOrNull{it.id==activeChatId}?:return
        chat.messages.takeLast(80).forEach{ui.addMessage(it.role,it.text)}
    }
    private fun showChats() {
        val all=chats.list().take(50)
        if(all.isEmpty())return
        val titles=all.map{it.title+" • "+it.messages.size+" сообщений"}.toTypedArray()
        AlertDialog.Builder(this).setTitle("Чаты").setItems(titles){_,which->
            activeChatId=all[which].id
            ui.clearMessages()
            all[which].messages.takeLast(80).forEach{ui.addMessage(it.role,it.text)}
        }.setPositiveButton("Новый чат"){_,_->newChat()}.show()
    }
    private fun showVoiceSettings() {
        val rate=SeekBar(this).apply{max=150;progress=((speechRate-0.5f)*100).toInt().coerceIn(0,150)}
        val pitch=SeekBar(this).apply{max=150;progress=((speechPitch-0.5f)*100).toInt().coerceIn(0,150)}
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(32,12,32,0)
            addView(android.widget.TextView(this@MainActivity).apply{text="Скорость речи"})
            addView(rate)
            addView(android.widget.TextView(this@MainActivity).apply{text="Высота голоса"})
            addView(pitch)
        }
        AlertDialog.Builder(this).setTitle("Настройки голоса JARVIS").setView(box)
            .setPositiveButton("Применить"){_,_->
                speechRate=0.5f+rate.progress/100f
                speechPitch=0.5f+pitch.progress/100f
                if(ttsReady){tts.setSpeechRate(speechRate);tts.setPitch(speechPitch);speak("Настройки голоса применены.")}
            }.setNegativeButton("Отмена",null).show()
    }
    private fun newChat() {
        activeChatId=chats.create().id
        ui.clearMessages()
        ui.addMessage("assistant","Новый чат создан.")
        chats.add(activeChatId,"assistant","Новый чат создан.")
    }
    private fun process(text:String) {
        ui.addMessage("user",text)
        chats.add(activeChatId,"user",text)
        val segments=planner.split(text)
        val results=mutableListOf<String>()
        for(segment in segments){
            val reply=engine.answer(segment)
            var response=reply.text
            if(reply.command!=null){
                val action=actions.execute(reply.command)
                if(action.message.isNotBlank()) response=if(action.ok)action.message else "Ошибка: "+action.message
            }
            if(response.isNotBlank())results+=response
        }
        val response=results.joinToString("\n").ifBlank{"Не удалось распознать команду."}
        ui.addMessage("assistant",response)
        chats.add(activeChatId,"assistant",response)
        speak(response)
    }
    private fun listen() {
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT,"Слушаю…")
        }
        try{startActivityForResult(intent,REQUEST_SPEECH)}catch(_:Exception){
            ui.addMessage("assistant","Ошибка: на устройстве нет доступного распознавания речи.")
        }
    }
    @Deprecated("Android compatibility callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=REQUEST_SPEECH||data==null)return
        val text=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?:return
        process(text)
    }
    private fun speak(text:String) {
        if(!ttsReady||text.isBlank())return
        tts.stop()
        tts.language=Locale("ru","RU")
        tts.setSpeechRate(speechRate)
        tts.setPitch(speechPitch)
        tts.speak(text.take(2500),TextToSpeech.QUEUE_FLUSH,null,"jarvis-response")
    }
    override fun onInit(status:Int) {
        ttsReady=status==TextToSpeech.SUCCESS
        if(ttsReady){
            tts.language=Locale("ru","RU")
            tts.setSpeechRate(speechRate)
            tts.setPitch(speechPitch)
        }
    }
    override fun onDestroy() {
        if(::tts.isInitialized){tts.stop();tts.shutdown()}
        super.onDestroy()
    }
    companion object{private const val REQUEST_SPEECH=7101}
}