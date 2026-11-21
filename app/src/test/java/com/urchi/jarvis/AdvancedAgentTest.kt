package com.urchi.jarvis

import com.urchi.jarvis.v4.advanced.*
import org.junit.Assert.*
import org.junit.Test

class AdvancedAgentTest {
    @Test fun planIsStableForExplicitGoogle(){ val p=AdvancedAgent().plan("открой google"); assertEquals(TaskKind.WEB,p.intent.kind); assertEquals("web.open",p.tools.single().id) }
    @Test fun missingExecutorIsNeverPresentedAsSuccess(){ val r=AdvancedAgent().execute("создай видео про космос"); assertFalse(r.ok); assertEquals("executor_missing",r.data["status"]) }
    @Test fun unknownTaskIsRejected(){ val r=AdvancedAgent().execute(""); assertFalse(r.ok); assertTrue(r.message.isNotBlank()) }
}
