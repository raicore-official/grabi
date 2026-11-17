package com.raisoft.grabi.data.vpn.autobot

import com.raisoft.grabi.data.vpn.ConfigFinder
import com.raisoft.grabi.domain.vpn.GenerationResult
import com.raisoft.grabi.domain.vpn.VpnProtocol
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCapabilitiesTest {

    @Test
    fun githubIsFullyAutomatable() {
        val source = GitHubConfigSource(ConfigFinder())
        val caps = source.capabilities
        assertTrue(caps.automaticDiscovery)
        assertTrue(caps.automaticGeneration)
        assertTrue(caps.automaticDownload)
        assertFalse(caps.requiresHumanVerification)
        assertEquals(setOf(VpnProtocol.WIREGUARD), caps.protocols)
        assertFalse(caps.expiryMetadata)
        assertEquals("github", source.id)
    }

    @Test
    fun vpnbookRequiresHumanVerification() {
        val source = VpnBookSource(OkHttpClient())
        val caps = source.capabilities
        assertTrue(caps.automaticDiscovery)
        assertFalse(caps.automaticGeneration)
        assertFalse(caps.automaticDownload)
        assertTrue(caps.requiresHumanVerification)
        assertEquals(setOf(VpnProtocol.WIREGUARD), caps.protocols)
        assertTrue(caps.expiryMetadata)
        assertEquals("vpnbook", source.id)
    }

    @Test
    fun vpnbookGenerateNeverBypassesCheck() = runBlocking {
        val source = VpnBookSource(OkHttpClient())
        val result = source.generate(country = "US")
        assertTrue(result is GenerationResult.HumanVerificationRequired)
        assertEquals(VpnBookSource.PAGE_URL, (result as GenerationResult.HumanVerificationRequired).url)
    }

    @Test
    fun vpnbookValidityIsSevenDays() {
        assertEquals(7L * 24 * 3_600_000, VpnBookSource.VALIDITY_MS)
    }
}