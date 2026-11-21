package com.urchi.jarvis

import org.junit.Assert.assertTrue
import org.junit.Test

class BrainKnowledgeTest {
    @Test
    fun pushkinMotherIsKnownInRussian() {
        val answer = BrainKnowledge.answer("Кем была мама Пушкина и как её звали")
        assertTrue(answer != null)
        assertTrue(answer!!.contains("Надежда Осиповна", ignoreCase = true))
    }

    @Test
    fun englishFactsAreKnown() {
        val answer = BrainKnowledge.answer("What is photosynthesis?")
        assertTrue(answer != null)
        assertTrue(answer!!.contains("Photosynthesis", ignoreCase = true))
    }

    @Test
    fun russianGrammarBasicsAreKnown() {
        val answer = BrainKnowledge.answer("Что такое деепричастный оборот?")
        assertTrue(answer != null)
        assertTrue(answer!!.contains("обособ", ignoreCase = true))
    }
}
