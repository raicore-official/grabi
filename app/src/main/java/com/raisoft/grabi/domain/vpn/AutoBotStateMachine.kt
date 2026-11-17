package com.raisoft.grabi.domain.vpn

enum class AutoBotPhase {
    OFF,
    IDLE,
    DISCOVERING,
    VALIDATING,
    CANDIDATE_READY,
    REPLACING,
    VERIFYING,
    ACTIVE,
    FAILED,
    NEEDS_USER,
}

enum class AutoBotTrigger {
    ENABLE,
    DISABLE,
    START,
    DISCOVERED,
    VALIDATED,
    NO_CANDIDATE,
    REPLACE,
    VERIFIED,
    VERIFY_FAILED,
    USER_REQUIRED,
    RETRY,
}

object AutoBotStateMachine {
    fun next(current: AutoBotPhase, trigger: AutoBotTrigger): AutoBotPhase = when (trigger) {
        AutoBotTrigger.DISABLE -> AutoBotPhase.OFF
        AutoBotTrigger.ENABLE -> AutoBotPhase.IDLE
        AutoBotTrigger.START -> if (current == AutoBotPhase.OFF) AutoBotPhase.OFF else AutoBotPhase.DISCOVERING
        AutoBotTrigger.DISCOVERED -> AutoBotPhase.VALIDATING
        AutoBotTrigger.VALIDATED -> AutoBotPhase.CANDIDATE_READY
        AutoBotTrigger.NO_CANDIDATE -> AutoBotPhase.NEEDS_USER
        AutoBotTrigger.REPLACE -> when (current) {
            AutoBotPhase.CANDIDATE_READY, AutoBotPhase.FAILED, AutoBotPhase.ACTIVE, AutoBotPhase.IDLE ->
                AutoBotPhase.REPLACING
            else -> current
        }
        AutoBotTrigger.VERIFIED -> AutoBotPhase.ACTIVE
        AutoBotTrigger.VERIFY_FAILED -> AutoBotPhase.FAILED
        AutoBotTrigger.USER_REQUIRED -> AutoBotPhase.NEEDS_USER
        AutoBotTrigger.RETRY -> if (current == AutoBotPhase.OFF) AutoBotPhase.OFF else AutoBotPhase.DISCOVERING
    }
}