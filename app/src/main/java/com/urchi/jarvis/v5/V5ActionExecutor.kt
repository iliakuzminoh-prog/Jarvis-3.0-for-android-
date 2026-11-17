package com.urchi.jarvis.v5

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.urchi.jarvis.JarvisAccessibilityService
import com.urchi.jarvis.v4.core.CommandKind
import com.urchi.jarvis.v4.core.ParsedCommand

class V5ActionExecutor(private val context: Context) {
    data class Result(val ok:Boolean,val message:String)
    fun execute(c:ParsedCommand):Result=when(c.kind){
        CommandKind.OPEN_APP->openApp(c.argument)
        CommandKind.SEARCH_WEB->searchWeb(c.argument)
        CommandKind.YOUTUBE->searchWeb("site:youtube.com " + c.argument)
        CommandKind.MEDIA_PLAY->media(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
        CommandKind.MEDIA_PAUSE->media(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
        CommandKind.MEDIA_NEXT->media(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        CommandKind.MEDIA_PREVIOUS->media(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        CommandKind.SETTINGS->device(c.argument)
        CommandKind.WIFI_SETTINGS->launch(Intent(Settings.ACTION_WIFI_SETTINGS),"Открыл настройки Wi‑Fi.")
        CommandKind.BLUETOOTH_SETTINGS->launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"Открыл настройки Bluetooth.")
        CommandKind.NOTIFICATION->JarvisAccessibilityService.instance?.let { a -> val ok=a.globalNotifications(); Result(ok,if(ok)"Открыл уведомления." else "Не удалось открыть уведомления.") } ?: Result(false,"Включи службу специальных возможностей JARVIS.")
        else->Result(false,"")
    }
    private fun openApp(query:String):Result{
        val pm=context.packageManager
        val list=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        val q=query.lowercase().replace('ё','е').replace("ё","е")
        val hit=list.firstOrNull{r->val l=r.loadLabel(pm).toString().lowercase().replace('ё','е');val p=r.activityInfo.packageName.lowercase();l==q||l.contains(q)||q.contains(l)||p.contains(q.replace(" ",""))}
            ?: return Result(false,"Не нашёл приложение «$query».")
        val i=pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return Result(false,"У приложения «$query» нет доступного запуска.")
        return try{context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Result(true,"Открыл "+hit.loadLabel(pm)+".")}catch(_:Exception){Result(false,"Не удалось открыть «$query».")}
    }
    private fun searchWeb(query:String):Result=try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Result(true,"Открыл поиск.")}catch(_:Exception){Result(false,"Не удалось открыть веб-поиск.")}
    private fun media(key:Int):Result=try{val a=context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager;a.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,key));a.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,key));Result(true,"Медиакоманда отправлена.")}catch(_:Exception){Result(false,"Не удалось отправить медиакоманду.")}
    private fun device(arg:String):Result{
        val a=JarvisAccessibilityService.instance
        return when(arg){
            "home"->a?.let{val ok=it.globalHome();Result(ok,if(ok)"Главный экран открыт." else "Не удалось открыть главный экран.")}?:disabled()
            "back"->a?.let{val ok=it.globalBack();Result(ok,if(ok)"Назад." else "Не удалось выполнить «назад».")}?:disabled()
            "quick_settings"->a?.let{val ok=it.globalQuickSettings();Result(ok,if(ok)"Открыл быстрые настройки." else "Не удалось открыть быстрые настройки.")}?:disabled()
            "settings"->try{context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Result(true,"Открыл настройки.")}catch(_:Exception){Result(false,"Не удалось открыть настройки.")}
            else->Result(false,"Команда распознана, но действие пока не реализовано.")
        }
    }
    private fun disabled()=Result(false,"Включи службу специальных возможностей JARVIS в системных настройках.")
    private fun launch(i:Intent,s:String):Boolean=try{context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}catch(_:Exception){false}
}
