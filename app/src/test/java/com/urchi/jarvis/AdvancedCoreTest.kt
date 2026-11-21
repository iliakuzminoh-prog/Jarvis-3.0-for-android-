package com.urchi.jarvis

import com.urchi.jarvis.v4.advanced.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AdvancedCoreTest {
    @Test fun routerSeparatesExplicitWebFromChat() {
        val r = AdvancedRouter()
        assertEquals(TaskKind.WEB, r.classify("найди в интернете погоду").kind)
        assertTrue(r.classify("открой google").explicitExternalNavigation)
        assertEquals(TaskKind.CHAT, r.classify("расскажи про космос").kind)
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
}
