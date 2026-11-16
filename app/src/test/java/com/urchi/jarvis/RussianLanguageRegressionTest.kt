package com.urchi.jarvis

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianLanguageRegressionTest {
    @Test fun answersSchoolDefinitionBeforeGenericConversationFallback() {
        val answer = LanguageCore.answer("что такое фотосинтез?")
        assertNotNull(answer)
        assertTrue(answer!!.contains("световую энергию"))
    }

    @Test fun answersPeriodicTableQuestionLocally() {
        val answer = LanguageCore.answer("таблица Менделеева")
        assertNotNull(answer)
        assertTrue(answer!!.contains("118"))
    }

    @Test fun recognizesEnglishGreeting() {
        assertTrue(RussianLanguageEngine.answer("hello")?.contains("Hello") == true)
    }

    @Test fun ordinaryKnowledgeQuestionDoesNotBecomeWebSearchIntent() {
        assertTrue(SmartCommandEngine.resolve("что такое фотосинтез") == null)
    }

    @Test fun combinesRussianTensAndUnitsInSpokenArithmetic() {
        assertTrue(RussianLanguageEngine.answer("двадцать три плюс пять")?.contains("28") == true)
    }

    @Test fun combinesEnglishTensAndUnitsInSpokenArithmetic() {
        assertTrue(RussianLanguageEngine.answer("twenty three plus five")?.contains("28") == true)
    }
}
