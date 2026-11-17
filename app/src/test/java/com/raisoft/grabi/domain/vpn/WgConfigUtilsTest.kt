package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WgConfigUtilsTest {

    private val privateKey = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
    private val publicKey = "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI="

    private fun clientText(endpoint: String? = "1.2.3.4:51820") = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = 10.0.0.2/32")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $publicKey")
        appendLine("AllowedIPs = 0.0.0.0/0")
        if (endpoint != null) appendLine("Endpoint = $endpoint")
    }

    @Test
    fun parsesValidClientConfig() {
        val config = WgConfigUtils.parse(clientText())
        assertTrue(WgConfigUtils.isUsableClient(config))
        assertEquals("1.2.3.4:51820", WgConfigUtils.endpointLabel(config))
        assertEquals(44, WgConfigUtils.publicKeyOf(config).length)
    }

    @Test
    fun rejectsGarbage() {
        assertTrue(runCatching { WgConfigUtils.parse("not a config") }.isFailure)
        assertTrue(runCatching { WgConfigUtils.parse("") }.isFailure)
    }

    @Test
    fun serverConfigWithoutEndpointIsNotUsable() {
        val config = WgConfigUtils.parse(clientText(endpoint = null))
        assertFalse(WgConfigUtils.isUsableClient(config))
        assertNull(WgConfigUtils.endpointLabel(config))
    }

    @Test
    fun excludedAppsRoundTrip() {
        val config = WgConfigUtils.parse(clientText())
        val apps = setOf("com.foo", "com.bar")
        val modified = WgConfigUtils.withExcludedApps(config, apps)
        assertEquals(apps, modified.getInterface().excludedApplications)
        assertTrue(WgConfigUtils.isUsableClient(modified))
        assertEquals("1.2.3.4:51820", WgConfigUtils.endpointLabel(modified))
        val cleared = WgConfigUtils.withExcludedApps(modified, emptySet())
        assertTrue(cleared.getInterface().excludedApplications.isEmpty())
    }

    @Test
    fun sanitizeNameKeepsAllowedCharacters() {
        assertEquals("My-Config--US", WgConfigUtils.sanitizeName("My Config (US)!"))
        assertEquals("grabi", WgConfigUtils.sanitizeName("!!!"))
        assertEquals("grabi", WgConfigUtils.sanitizeName(""))
        assertEquals(15, WgConfigUtils.sanitizeName("a-very-long-config-name-here").length)
        assertEquals("wg0.conf", WgConfigUtils.sanitizeName("wg0.conf"))
    }
}