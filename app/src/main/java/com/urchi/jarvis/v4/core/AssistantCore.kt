package com.urchi.jarvis.v4.core

import android.content.Context

class AssistantCore(context: Context) {
    private val memory = MemoryStore(context)
    private val router = CommandRouter()

    fun handle(text: String): AssistantReply {
        val parsed = router.parse(text)
        return when (parsed.kind) {
            CommandKind.MEMORY_REMEMBER -> {
                if (parsed.argument.isBlank()) reply("Что именно запомнить?", parsed)
                else {
                    memory.remember(parsed.argument)
                    reply("Запомнил: " + parsed.argument, parsed, true)
                }
            }
            CommandKind.MEMORY_FORGET -> {
                val count = memory.forget(parsed.argument)
                reply(
                    if (count > 0) "Удалил из памяти: " + parsed.argument
                    else "Такой записи в памяти нет.",
                    parsed, count > 0
                )
            }
            CommandKind.MEMORY_SEARCH -> {
                val recent = memory.recent(6)
                val result = if (recent.isEmpty()) "Память пока пуста."
                else "Последнее, что я помню: " +
                    recent.joinToString("; ") { it.text }
                reply(result, parsed)
            }
            CommandKind.HELP -> reply(
                "Я могу управлять подключёнными функциями телефона, работать с памятью, " +
                    "медиа, приложениями и настройками. Веб-поиск выполняется только " +
                    "по явной команде пользователя.",
                parsed
            )
            CommandKind.CONVERSATION -> reply(
                "Я понял это как разговорный запрос, а не как команду устройству.",
                parsed
            )
            else -> reply(
                "Команда распознана: " + parsed.kind.name.lowercase() +
                    ". Исполнитель этой функции будет подключён отдельным модулем.",
                parsed
            )
        }
    }

    fun memories(): List<MemoryStore.Memory> = memory.recent(50)

    private fun reply(text: String, command: ParsedCommand, action: Boolean = false) =
        AssistantReply(text, text, command, action)
}
