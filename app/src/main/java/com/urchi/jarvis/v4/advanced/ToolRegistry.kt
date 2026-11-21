package com.urchi.jarvis.v4.advanced

class ToolRegistry {
    private val tools = linkedMapOf<String, ToolSpec>()

    init {
        register(ToolSpec("chat", "Ответить через выбранную AI-модель"))
        register(ToolSpec("web.search", "Поиск актуальной информации в интернете", requiresNetwork = true))
        register(ToolSpec("web.open", "Открыть сайт только по явной команде пользователя", requiresNetwork = true))
        register(ToolSpec("android.app", "Запустить установленное приложение"))
        register(ToolSpec("android.settings", "Открыть системные настройки"))
        register(ToolSpec("media.image", "Генерация или анализ изображений"))
        register(ToolSpec("media.video", "Генерация или анализ видео", requiresNetwork = true))
        register(ToolSpec("media.audio", "Генерация или анализ аудио", requiresNetwork = true))
        register(ToolSpec("files.inspect", "Определить тип и свойства локального файла"))
        register(ToolSpec("code.generate", "Создать или исправить исходный код"))
        register(ToolSpec("build.dispatch", "Подготовить или передать проект на сборку", requiresNetwork = true))
        register(ToolSpec("memory", "Сохранить, найти или удалить память"))
        register(ToolSpec("tiktok.api", "Работа через официальные TikTok API", requiresNetwork = true))
    }

    fun register(spec: ToolSpec) { tools[spec.id] = spec }
    fun get(id: String): ToolSpec? = tools[id]
    fun all(): List<ToolSpec> = tools.values.toList()
}
