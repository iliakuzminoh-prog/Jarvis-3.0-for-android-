package com.urchi.jarvis.v4.advanced

class AdvancedAgent(private val planner: AgentPlanner = AgentPlanner(), private val registry: CapabilityRegistry = CapabilityRegistry()) {
    fun plan(text: String): AgentPlan = planner.plan(text)
    fun execute(text: String): CapabilityResult = execute(plan(text))
    fun execute(plan: AgentPlan): CapabilityResult {
        if (plan.intent.kind == TaskKind.UNKNOWN) return CapabilityResult(false, "Не удалось определить задачу.")
        val tool = plan.tools.firstOrNull() ?: return CapabilityResult(false, "Для этой задачи нет зарегистрированного инструмента.")
        if (plan.requiresConfirmation) return CapabilityResult(false, "Требуется подтверждение перед выполнением: ${tool.id}", mapOf("status" to "confirmation_required", "tool" to tool.id))
        val capability = registry.find(plan.intent) ?: return CapabilityResult(false, "Исполнитель ${tool.id} не подключён.", mapOf("status" to "executor_missing", "tool" to tool.id))
        return try { capability.execute(plan.intent) } catch (t: Throwable) { CapabilityResult(false, "Исполнитель ${capability.id} завершился с ошибкой: ${t.message ?: t::class.simpleName.orEmpty()}", mapOf("status" to "executor_error", "tool" to capability.id)) }
    }
}
