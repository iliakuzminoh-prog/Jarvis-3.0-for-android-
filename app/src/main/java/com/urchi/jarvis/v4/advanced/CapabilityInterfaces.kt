package com.urchi.jarvis.v4.advanced

data class CapabilityResult(val ok:Boolean,val message:String,val data:Map<String,String> = emptyMap())

interface JarvisCapability {
    val id:String
    fun canHandle(intent:TaskIntent):Boolean
    fun describe():ToolSpec
    fun execute(intent:TaskIntent):CapabilityResult
}

class CapabilityRegistry {
    private val items=linkedMapOf<String,JarvisCapability>()
    fun register(capability:JarvisCapability){ items[capability.id]=capability }
    fun find(intent:TaskIntent):JarvisCapability?=items.values.firstOrNull{it.canHandle(intent)}
    fun all():List<JarvisCapability> = items.values.toList()
}
