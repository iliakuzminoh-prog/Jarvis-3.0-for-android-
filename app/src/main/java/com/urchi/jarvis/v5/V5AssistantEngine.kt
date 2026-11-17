package com.urchi.jarvis.v5

import android.content.Context
import com.urchi.jarvis.ConversationEngine
import com.urchi.jarvis.LanguageCore
import com.urchi.jarvis.RussianLanguageEngine
import com.urchi.jarvis.v4.core.AssistantReply
import com.urchi.jarvis.v4.core.CommandKind
import com.urchi.jarvis.v4.core.MemoryStore

class V5AssistantEngine(context: Context) {
    private val memory = MemoryStore(context)
    private val recognizer = V5IntentRecognizer()
    fun answer(raw: String): AssistantReply {
        val parsed = recognizer.recognize(raw)
        val response = when (parsed.kind) {
            CommandKind.MEMORY_REMEMBER -> if (parsed.argument.isBlank()) "Что именно запомнить?" else { memory.remember(parsed.argument); "Запомнил: " + parsed.argument }
            CommandKind.MEMORY_FORGET -> { val count=memory.forget(parsed.argument); if(count>0) "Удалил из памяти $count записи." else "Не нашёл такую запись в памяти." }
            CommandKind.MEMORY_SEARCH -> { val r=memory.recent(8); if(r.isEmpty()) "Память пока пуста." else "Последние записи:\n" + r.joinToString("\n"){ "• " + it.text } }
            CommandKind.SCHOOL, CommandKind.CALCULATE -> LanguageCore.answer(parsed.original) ?: "Я распознал учебный запрос, но локальный решатель не поддерживает этот тип задачи."
            CommandKind.CONVERSATION -> ConversationEngine.answer(parsed.original) ?: RussianLanguageEngine.answer(parsed.original) ?: "Я понял фразу, но для точного ответа нужен подключённый источник знаний или языковая модель."
            else -> null
        }
        return AssistantReply(response ?: "", command=parsed)
    }
    fun memoryStore(): MemoryStore = memory
}
