package com.urchi.jarvis.v4.advanced

class AdvancedAgent(
    private val planner:AgentPlanner = AgentPlanner(),
    private val registry:CapabilityRegistry = CapabilityRegistry()
){
    fun plan(text:String):AgentPlan=planner.plan(text)
    fun execute(text:String):CapabilityResult{
        val plan=plan(text)
        if(plan.tools.isEmpty()) return CapabilityResult(false,"Подходящий инструмент не зарегистрирован.")
        if(plan.requiresConfirmation) return CapabilityResult(false,"Требуется подтверждение перед выполнением: "+plan.tools.joinToString{it.id})
        val cap=registry.find(plan.intent)
            ?: return CapabilityResult(false,"Инструмент "+plan.tools.first().id+" ещё не подключён к исполнителю.")
        return cap.execute(plan.intent)
    }
}
