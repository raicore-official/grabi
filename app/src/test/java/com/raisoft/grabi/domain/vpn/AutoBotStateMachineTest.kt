package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoBotStateMachineTest {

    @Test
    fun enableFromOff() {
        assertEquals(AutoBotPhase.IDLE, AutoBotStateMachine.next(AutoBotPhase.OFF, AutoBotTrigger.ENABLE))
    }

    @Test
    fun startIgnoredWhileOff() {
        assertEquals(AutoBotPhase.OFF, AutoBotStateMachine.next(AutoBotPhase.OFF, AutoBotTrigger.START))
        assertEquals(AutoBotPhase.OFF, AutoBotStateMachine.next(AutoBotPhase.OFF, AutoBotTrigger.RETRY))
    }

    @Test
    fun happyPath() {
        var phase = AutoBotPhase.IDLE
        phase = AutoBotStateMachine.next(phase, AutoBotTrigger.START)
        assertEquals(AutoBotPhase.DISCOVERING, phase)
        phase = AutoBotStateMachine.next(phase, AutoBotTrigger.DISCOVERED)
        assertEquals(AutoBotPhase.VALIDATING, phase)
        phase = AutoBotStateMachine.next(phase, AutoBotTrigger.VALIDATED)
        assertEquals(AutoBotPhase.CANDIDATE_READY, phase)
        phase = AutoBotStateMachine.next(phase, AutoBotTrigger.REPLACE)
        assertEquals(AutoBotPhase.REPLACING, phase)
        phase = AutoBotStateMachine.next(phase, AutoBotTrigger.VERIFIED)
        assertEquals(AutoBotPhase.ACTIVE, phase)
    }

    @Test
    fun replaceOnlyFromAllowedPhases() {
        assertEquals(
            AutoBotPhase.DISCOVERING,
            AutoBotStateMachine.next(AutoBotPhase.DISCOVERING, AutoBotTrigger.REPLACE),
        )
        assertEquals(
            AutoBotPhase.REPLACING,
            AutoBotStateMachine.next(AutoBotPhase.FAILED, AutoBotTrigger.REPLACE),
        )
        assertEquals(
            AutoBotPhase.REPLACING,
            AutoBotStateMachine.next(AutoBotPhase.ACTIVE, AutoBotTrigger.REPLACE),
        )
    }

    @Test
    fun failureAndRetry() {
        assertEquals(AutoBotPhase.FAILED, AutoBotStateMachine.next(AutoBotPhase.REPLACING, AutoBotTrigger.VERIFY_FAILED))
        assertEquals(AutoBotPhase.DISCOVERING, AutoBotStateMachine.next(AutoBotPhase.FAILED, AutoBotTrigger.RETRY))
    }

    @Test
    fun noCandidateNeedsUser() {
        assertEquals(AutoBotPhase.NEEDS_USER, AutoBotStateMachine.next(AutoBotPhase.VALIDATING, AutoBotTrigger.NO_CANDIDATE))
        assertEquals(AutoBotPhase.NEEDS_USER, AutoBotStateMachine.next(AutoBotPhase.DISCOVERING, AutoBotTrigger.USER_REQUIRED))
    }

    @Test
    fun disableAlwaysOff() {
        AutoBotPhase.entries.forEach { phase ->
            assertEquals(AutoBotPhase.OFF, AutoBotStateMachine.next(phase, AutoBotTrigger.DISABLE))
        }
    }
}