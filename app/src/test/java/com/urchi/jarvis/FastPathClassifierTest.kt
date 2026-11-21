package com.urchi.jarvis

import org.junit.Assert.assertEquals
import org.junit.Test

class FastPathClassifierTest {
    @Test
    fun shortQuestionUsesFastPath() {
        assertEquals(FastPathClassifier.Level.FAST, FastPathClassifier.classify("Кто написал Евгения Онегина?"))
    }

    @Test
    fun codingTaskUsesDeepPath() {
        assertEquals(FastPathClassifier.Level.DEEP, FastPathClassifier.classify("Напиши код приложения на Kotlin"))
    }

    @Test
    fun longRequestUsesDeepPath() {
        val request = "Составь подробный план архитектуры большого приложения, учти память, сеть, безопасность, тесты, отмену задач, обработку ошибок, несколько исполнителей и этапы сборки."
        assertEquals(FastPathClassifier.Level.DEEP, FastPathClassifier.classify(request))
    }
}
