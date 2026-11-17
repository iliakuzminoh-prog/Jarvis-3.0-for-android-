package com.urchi.jarvis.v5

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import com.urchi.jarvis.ChatStore

class V5Ui(private val context:Context,private val onSend:(String)->Unit,private val onSpeak:()->Unit,private val onNewChat:()->Unit,private val onPermissions:()->Unit){
    lateinit var root:LinearLayout
    lateinit var messages:LinearLayout
    lateinit var input:EditText
    private val bg=Color.rgb(8,13,20);private val card=Color.rgb(18,27,38);private val accent=Color.rgb(83,215,194)
    fun build():View{
        root=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(18,18,18,12)}
        val header=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        header.addView(TextView(context).apply{text="JARVIS V5";textSize=27f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Button(context).apply{text="Доступы";setOnClickListener{onPermissions()}})
        root.addView(header)
        val nav=LinearLayout(context).apply{gravity=Gravity.CENTER}
        nav.addView(button("Новый чат"){onNewChat()},LinearLayout.LayoutParams(0,-2,1f));nav.addView(button("Голос"){onSpeak()},LinearLayout.LayoutParams(0,-2,1f))
        root.addView(nav)
        root.addView(TextView(context).apply{text="ЯДРО V5 • ЯЗЫК • ПАМЯТЬ • ДЕЙСТВИЯ";textSize=12f;setTextColor(accent);setPadding(12,12,12,12);setBackgroundColor(card)})
        val scroll=ScrollView(context);messages=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(0,10,0,10)};scroll.addView(messages);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val bar=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        input=EditText(context).apply{hint="Команда или вопрос…";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);maxLines=4}
        bar.addView(input,LinearLayout.LayoutParams(0,-2,1f));bar.addView(Button(context).apply{text="➤";setOnClickListener{send()}},LinearLayout.LayoutParams(64,-2));root.addView(bar)
        return root
    }
    private fun send(){val t=input.text.toString().trim();if(t.isNotBlank()){input.text.clear();onSend(t)}}
    fun addMessage(role:String,text:String){messages.addView(TextView(context).apply{text=if(role=="user")"Ты\n$text" else "JARVIS\n$text";textSize=16f;setTextColor(Color.WHITE);setPadding(16,12,16,12);setBackgroundColor(if(role=="user")Color.rgb(25,40,51)else card)},LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,6,0,6)})}
    fun clearMessages(){messages.removeAllViews()}
    private fun button(label:String,action:()->Unit)=Button(context).apply{text=label;setOnClickListener{action()}}
}
