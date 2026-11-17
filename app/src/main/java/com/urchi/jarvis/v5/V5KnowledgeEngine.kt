package com.urchi.jarvis.v5

object V5KnowledgeEngine {
    fun answer(text:String):String? = KnowledgeBase.find(text)?.answer
}
