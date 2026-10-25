package app.intentional.pause.core

import org.junit.Assert.*
import org.junit.Test

class SessionMachineTest {
    @Test fun firstOpenNeedsAnAnswer() {
        val s = SessionMachine(); s.foreground("instagram", true)
        assertTrue(s.prompting); assertFalse(s.counting)
        assertTrue(s.approve()); assertTrue(s.counting); assertFalse(s.prompting)
    }
    @Test fun repeatedEventsDoNotRepeatPromptOrOverlap() {
        val s = SessionMachine(); s.foreground("a", true); s.approve()
        repeat(100) { s.foreground("a", true) }
        assertTrue(s.counting); assertEquals("a", s.app); assertFalse(s.approve())
    }
    @Test fun switchingMonitoredAppsAlwaysNeedsSeparateApproval() {
        val s = SessionMachine(); s.foreground("a", true); s.approve(); s.foreground("b", true)
        assertTrue(s.prompting); assertFalse(s.counting)
        s.approve(); s.foreground("a", true)
        assertTrue(s.prompting)
    }
    @Test fun homeAndReturnBeginAnotherSession() {
        val s = SessionMachine(); s.foreground("a", true); s.approve(); s.foreground("launcher", false)
        assertNull(s.app); assertFalse(s.counting)
        s.foreground("a", true); assertTrue(s.prompting)
    }
    @Test fun temporarySystemWindowPausesWithoutLosingIntention() {
        val s = SessionMachine(); s.foreground("a", true); s.approve(); s.interrupt()
        assertFalse(s.counting); assertFalse(s.prompting)
        s.foreground("a", true); assertTrue(s.counting); assertFalse(s.prompting)
    }
    @Test fun interruptedPromptCannotBeApprovedAndReturnsSafely() {
        val s = SessionMachine(); s.foreground("a", true); s.interrupt()
        assertFalse(s.approve()); s.foreground("a", true); assertTrue(s.prompting)
    }
    @Test fun lockOrDisabledMonitoringClearsApproval() {
        val s = SessionMachine(); s.foreground("a", true); s.approve(); s.leave()
        assertFalse(s.counting); s.foreground("a", true); assertTrue(s.prompting)
    }
    @Test fun rapidSwitchingHasOnlyOneOwner() {
        val s = SessionMachine()
        repeat(1000) { s.foreground("app-${it % 3}", true); assertTrue(s.prompting); s.approve(); assertTrue(s.counting) }
        assertEquals("app-0", s.app)
    }
}
