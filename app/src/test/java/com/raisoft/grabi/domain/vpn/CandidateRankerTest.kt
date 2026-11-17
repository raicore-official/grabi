package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateRankerTest {

    private val now = 1_000_000_000_000L
    private val hour = 3_600_000L
    private val priority = listOf("github", "vpnbook")

    private fun config(
        provider: String,
        country: String = "US",
        text: String = "[Interface]$provider$country",
        generatedAt: Long = now,
    ) = RemoteVpnConfig(
        providerId = provider,
        name = "$provider-$country",
        country = country,
        protocol = VpnProtocol.WIREGUARD,
        configText = text,
        endpoint = "host:443",
        generatedAt = generatedAt,
        expiresAt = null,
        expirySource = "none",
    )

    private fun rank(
        candidates: List<RemoteVpnConfig>,
        preferred: String = "",
        country: String = "",
        records: Map<String, ExpiryRecord> = emptyMap(),
    ) = CandidateRanker.rank(candidates, priority, preferred, country, { records[it] }, now)

    private fun record(expiresAt: Long) = ExpiryRecord(
        configKey = "k",
        generatedAt = now - hour,
        expiresAt = expiresAt,
        providerExpiryPolicy = "7d",
        expirySource = "provider",
        lastCheckedAt = now,
    )

    @Test
    fun priorityOrderWinsByDefault() {
        val ranked = rank(listOf(config("vpnbook"), config("github")))
        assertEquals("github", ranked[0].providerId)
    }

    @Test
    fun preferredProviderOverridesPriority() {
        val ranked = rank(listOf(config("github"), config("vpnbook")), preferred = "vpnbook")
        assertEquals("vpnbook", ranked[0].providerId)
    }

    @Test
    fun countryPreferenceBoostsMatch() {
        val ranked = rank(
            listOf(config("github", country = "US"), config("github", country = "DE")),
            country = "de",
        )
        assertEquals("DE", ranked[0].country)
    }

    @Test
    fun expiredCandidatesExcluded() {
        val expired = config("github", text = "expired-config")
        val fresh = config("vpnbook", text = "fresh-config")
        val records = mapOf(ConfigKeys.of(expired.configText) to record(now - hour))
        val ranked = rank(listOf(expired, fresh), records = records)
        assertEquals(1, ranked.size)
        assertEquals("vpnbook", ranked[0].providerId)
    }

    @Test
    fun blankConfigsExcluded() {
        val blank = config("github").copy(configText = "  ")
        val ranked = rank(listOf(blank, config("vpnbook")))
        assertEquals(1, ranked.size)
    }

    @Test
    fun recencyAloneDoesNotReorder() {
        val older = config("github", text = "older", generatedAt = now - 100 * hour)
        val newer = config("github", text = "newer", generatedAt = now)
        val ranked = rank(listOf(older, newer))
        assertEquals("older", ranked[0].configText)
    }

    @Test
    fun longerRemainingLifeScoresHigher() {
        val short = config("github", text = "short")
        val long = config("github", text = "long")
        val records = mapOf(
            ConfigKeys.of(short.configText) to record(now + hour),
            ConfigKeys.of(long.configText) to record(now + 48 * hour),
        )
        val ranked = rank(listOf(short, long), records = records)
        assertEquals("long", ranked[0].configText)
        assertTrue(
            CandidateRanker.score(long, priority, "", "", { records[it] }, now) >
                CandidateRanker.score(short, priority, "", "", { records[it] }, now),
        )
    }
}