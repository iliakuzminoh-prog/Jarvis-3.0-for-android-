package com.urchi.jarvis

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianLanguageRegressionTest {
    @Test fun answersSchoolDefinitionFromSchoolKnowledge() {
        val answer = SchoolKnowledge.answer("что такое фотосинтез?")
        assertNotNull(answer)
        assertTrue(answer!!.contains("световой энергии"))
    }

    @Test fun answersPeriodicTableQuestionLocally() {
        val answer = LanguageCore.answer("таблица Менделеева")
        assertNotNull(answer)
        assertTrue(answer!!.contains("118"))
    }

    @Test fun answersEnglishSchoolQuestionInEnglish() {
        val answer = SchoolKnowledge.answer("What is photosynthesis?")
        assertNotNull(answer)
        assertTrue(answer!!.startsWith("Photosynthesis"))
    }

    @Test fun answersRussianGrammarQuestion() {
        val answer = SchoolKnowledge.answer("Сколько падежей в русском языке?")
        assertNotNull(answer)
        assertTrue(answer!!.contains("шесть"))
    }

    @Test fun answersEnglishGrammarQuestion() {
        val answer = SchoolKnowledge.answer("What is a noun?")
        assertNotNull(answer)
        assertTrue(answer!!.startsWith("A noun"))
    }

    @Test fun recognizesEnglishGreeting() {
        assertTrue(RussianLanguageEngine.answer("hello")?.contains("Hello") == true)
    }

    @Test fun recognizesGreetingWithPunctuation() {
        assertTrue(RussianLanguageEngine.answer("привет!")?.contains("Привет") == true)
        assertTrue(RussianLanguageEngine.answer("hello!")?.contains("Hello") == true)
    }

    @Test fun ordinaryKnowledgeQuestionDoesNotBecomeWebSearchIntent() {
        assertTrue(SmartCommandEngine.resolve("что такое фотосинтез") == null)
    }

    @Test fun conversationTemplatesDoNotInterceptSchoolQuestions() {
        assertTrue(RussianLanguageEngine.answer("что такое фотосинтез?") == null)
        assertTrue(RussianLanguageEngine.answer("что такое существительное?") == null)
        assertTrue(RussianLanguageEngine.answer("what is photosynthesis?") == null)
    }

    @Test fun combinesRussianTensAndUnitsInSpokenArithmetic() {
        assertTrue(MathSolver.solve("двадцать три плюс пять")?.contains("28") == true)
    }

    @Test fun combinesEnglishTensAndUnitsInSpokenArithmetic() {
        assertTrue(MathSolver.solve("twenty three plus five")?.contains("28") == true)
    }

    @Test fun evaluatesTheWholeExpressionInsteadOfOnlyTheFirstOperation() {
        assertTrue(MathSolver.solve("7 - 3 + 2")?.contains("6") == true)
    }
}
