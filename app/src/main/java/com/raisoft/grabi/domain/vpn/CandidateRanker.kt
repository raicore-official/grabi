package com.raisoft.grabi.domain.vpn

object CandidateRanker {

    fun rank(
        candidates: List<RemoteVpnConfig>,
        priority: List<String>,
        preferredProvider: String,
        countryPref: String,
        expiryOf: (String) -> ExpiryRecord?,
        now: Long,
    ): List<RemoteVpnConfig> =
        candidates
            .filter { it.configText.isNotBlank() }
            .filter {
                ExpiryCalculator.status(expiryOf(ConfigKeys.of(it.configText)), now) != ExpiryStatus.EXPIRED
            }
            .sortedByDescending { score(it, priority, preferredProvider, countryPref, expiryOf, now) }

    fun score(
        config: RemoteVpnConfig,
        priority: List<String>,
        preferredProvider: String,
        countryPref: String,
        expiryOf: (String) -> ExpiryRecord?,
        now: Long,
    ): Int {
        var score = 0
        val index = priority.indexOf(config.providerId)
        score += (priority.size - index.coerceAtLeast(0)) * 10
        if (preferredProvider.isNotBlank() && config.providerId == preferredProvider) score += 100
        if (countryPref.isNotBlank() && config.country.equals(countryPref, ignoreCase = true)) score += 40
        val record = expiryOf(ConfigKeys.of(config.configText))
        val remaining = ExpiryCalculator.remainingMs(record, now)
        if (remaining != null && remaining > 0) {
            score += (remaining / 3_600_000).toInt().coerceAtMost(24)
        }
        return score
    }
}