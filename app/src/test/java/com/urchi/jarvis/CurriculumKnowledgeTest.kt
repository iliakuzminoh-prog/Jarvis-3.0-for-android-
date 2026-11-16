package com.urchi.jarvis

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumKnowledgeTest {
    @Test fun databaseContainsManyCurriculumTopics() {
        assertTrue(CurriculumKnowledge.topicCount() >= 60)
    }

    @Test fun russianTopicIsResolved() {
        assertNotNull(CurriculumKnowledge.answer("что такое теорема Пифагора"))
    }

    @Test fun englishTopicIsResolved() {
        assertNotNull(CurriculumKnowledge.answer("what is present perfect"))
    }

    @Test fun mathTopicIsResolved() {
        assertNotNull(CurriculumKnowledge.answer("объясни квадратное уравнение"))
    }
}
