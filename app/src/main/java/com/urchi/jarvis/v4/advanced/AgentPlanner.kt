package com.urchi.jarvis.v4.advanced

class AgentPlanner(private val router: AdvancedRouter = AdvancedRouter(),
                   private val registry: ToolRegistry = ToolRegistry()) {
    fun plan(text: String): AgentPlan {
        val intent = router.classify(text)
        val toolId = when (intent.kind) {
            TaskKind.WEB -> if (intent.argument == "google") "web.open" else "web.search"
            TaskKind.APP, TaskKind.DEVICE -> "android.app"
            TaskKind.IMAGE -> "media.image"
            TaskKind.VIDEO -> "media.video"
            TaskKind.AUDIO -> "media.audio"
            TaskKind.FILE -> "files.inspect"
            TaskKind.CODE -> "code.generate"
            TaskKind.BUILD -> "build.dispatch"
            TaskKind.MEMORY -> "memory"
            TaskKind.TIKTOK -> "tiktok.api"
            else -> "chat"
        }
        val tool = registry.get(toolId)
        val confirmation = tool?.dangerous == true
        val reason = when {
            intent.kind == TaskKind.WEB && intent.explicitExternalNavigation ->
                "Внешний сайт открывается только потому, что пользователь попросил об этом явно."
            intent.kind == TaskKind.BUILD ->
                "Сборка требует отдельного исполнителя; телефон не должен притворяться универсальным компилятором."
            intent.kind == TaskKind.TIKTOK ->
                "Используется официальный API; без OAuth/разрешений аккаунта функция не активируется."
            else -> "Выбран наиболее подходящий подключённый инструмент."
        }
        return AgentPlan(intent,listOfNotNull(tool),confirmation,reason)
    }
}
