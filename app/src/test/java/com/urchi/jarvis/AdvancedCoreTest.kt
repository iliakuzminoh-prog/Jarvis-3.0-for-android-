package com.urchi.jarvis

import com.urchi.jarvis.v4.advanced.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AdvancedCoreTest {
    @Test fun routerSeparatesExplicitWebFromChat() {
        val r = AdvancedRouter()
        assertEquals(TaskKind.WEB, r.classify("найди в интернете погоду в Санкт-Петербурге").kind)
        assertEquals("погоду в санкт-петербурге", r.classify("найди в интернете погоду в Санкт-Петербурге").argument)
        assertTrue(r.classify("открой google").explicitExternalNavigation)
        assertEquals(TaskKind.CHAT, r.classify("расскажи про космос").kind)
    }

    @Test fun routerPreservesMediaPrompts() {
        val r = AdvancedRouter()
        assertEquals("космический корабль над лесом", r.classify("сгенерируй картинку космический корабль над лесом").argument)
        assertEquals("спокойная музыка для сна", r.classify("создай аудио спокойная музыка для сна").argument)
        assertEquals("город будущего ночью", r.classify("создай видео город будущего ночью").argument)
    }

    @Test fun routerPreservesFileArgument() {
        assertEquals("/storage/emulated/0/Download/test video.mp4",
            AdvancedRouter().classify("проанализируй видео /storage/emulated/0/Download/test video.mp4").argument)
    }

    @Test fun plannerSelectsTools() {
        val p = AgentPlanner().plan("создай видео про космос")
        assertEquals("media.video", p.tools.single().id)
    }

    @Test fun fileClassificationIsDeterministic() {
        assertEquals("Android package", FileCapability.classify("apk"))
        assertEquals("Windows executable", FileCapability.classify("exe"))
        assertEquals("Video", FileCapability.classify("mp4"))
    }

    @Test fun buildValidationRejectsEmptySource() {
        assertTrue(BuildCapability.validate(BuildRequest("x","kotlin","apk","")).isFailure)
        assertTrue(BuildCapability.needsRemoteToolchain(BuildRequest("x","c","exe","source")))
    }

    @Test fun fileInspectionDoesNotInventMetadata() {
        val f = File("missing-jarvis-test-file.apk")
        val info = FileCapability.inspect(f)
        assertFalse(info.exists)
        assertEquals("apk", info.extension)
    }
    @Test fun routerRecognizesEnglishQuestionsAndCodeRequests() {
        val r = AdvancedRouter()
        assertEquals(TaskKind.CHAT, r.classify("What is photosynthesis?").kind)
        assertEquals(TaskKind.CHAT, r.classify("Why is the sky blue?").kind)
        assertEquals(TaskKind.CODE, r.classify("write code for a Kotlin calculator").kind)
        assertEquals(TaskKind.CODE, r.classify("исправь код в этом файле").kind)
    }

    @Test fun routerKeepsFullNaturalQuestion() {
        val q = "Кем была мама Александра Пушкина и как её звали?"
        val intent = AdvancedRouter().classify(q)
        assertEquals(TaskKind.CHAT, intent.kind)
        assertEquals(q, intent.argument)
    }

}

