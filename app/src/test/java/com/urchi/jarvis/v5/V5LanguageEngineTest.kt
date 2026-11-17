package com.urchi.jarvis.v5

import com.urchi.jarvis.v4.core.CommandKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V5LanguageEngineTest {
    private val recognizer = V5IntentRecognizer()

    @Test fun russianInflectionRecognizesWifi() {
        assertEquals(CommandKind.WIFI_SETTINGS, recognizer.recognize("Открой настройки вайфая").kind)
    }

    @Test fun youtubeQueryIsExtracted() {
        val c = recognizer.recognize("Включи на Ютубе музыку для дороги")
        assertEquals(CommandKind.YOUTUBE, c.kind)
        assertEquals("музыку для дороги", c.argument)
    }

    @Test fun memoryCommandIsNotConversation() {
        val c = recognizer.recognize("Запомни что я люблю Minecraft")
        assertEquals(CommandKind.MEMORY_REMEMBER, c.kind)
        assertTrue(c.argument.contains("люблю"))
    }

    @Test fun plannerKeepsOrder() {
        val p = V5Planner().split("включи музыку, а потом открой Telegram и затем найди в интернете погоду")
        assertEquals(3, p.size)
        assertTrue(p[0].startsWith("включи"))
        assertTrue(p[1].startsWith("открой"))
        assertTrue(p[2].startsWith("найди"))
    }
}
