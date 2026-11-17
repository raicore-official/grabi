package com.raisoft.grabi.domain.vpn

enum class ProviderErrorKind { NETWORK, RATE_LIMITED, PARSE, PROVIDER, UNSUPPORTED }

data class ProviderAvailability(val available: Boolean, val detail: String)

sealed interface GenerationResult {
    data class Success(val config: RemoteVpnConfig) : GenerationResult
    data class HumanVerificationRequired(val url: String, val reason: String) : GenerationResult
    data class Unavailable(val reason: String) : GenerationResult
    data class Error(val kind: ProviderErrorKind, val message: String) : GenerationResult
}