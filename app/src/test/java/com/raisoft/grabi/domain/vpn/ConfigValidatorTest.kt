package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigValidatorTest {

    private val privateKey = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
    private val publicKey = "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI="

    private fun validText(
        endpoint: String? = "demo.example.com:51820",
        allowedIps: String? = "0.0.0.0/0",
    ) = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = 10.0.0.2/32")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $publicKey")
        if (allowedIps != null) appendLine("AllowedIPs = $allowedIps")
        if (endpoint != null) appendLine("Endpoint = $endpoint")
    }

    @Test
    fun validConfigPasses() {
        val result = ConfigValidator.validate(validText())
        assertTrue(result is ConfigValidator.Result.Valid)
        val valid = result as ConfigValidator.Result.Valid
        assertEquals(publicKey, valid.publicKey)
        assertEquals("demo.example.com:51820", valid.endpoint)
        assertEquals(listOf("0.0.0.0/0"), valid.allowedIps)
    }

    @Test
    fun emptyConfigRejected() {
        assertTrue(ConfigValidator.validate("  ") is ConfigValidator.Result.Invalid)
    }

    @Test
    fun missingSectionsRejected() {
        val result = ConfigValidator.validate("[Interface]\nPrivateKey = $privateKey\n")
        assertTrue(result is ConfigValidator.Result.Invalid)
    }

    @Test
    fun malformedKeyRejected() {
        val text = validText().replace(privateKey, "not-a-valid-key")
        assertTrue(ConfigValidator.validate(text) is ConfigValidator.Result.Invalid)
    }

    @Test
    fun missingEndpointRejected() {
        val result = ConfigValidator.validate(validText(endpoint = null))
        assertTrue(result is ConfigValidator.Result.Invalid)
        assertEquals("peer has no endpoint", (result as ConfigValidator.Result.Invalid).reason)
    }

    @Test
    fun missingAllowedIpsRejected() {
        val result = ConfigValidator.validate(validText(allowedIps = null))
        assertTrue(result is ConfigValidator.Result.Invalid)
    }

    @Test
    fun duplicateKeyCombinesPublicKeyAndEndpoint() {
        assertEquals("$publicKey|host:443", ConfigValidator.duplicateKey(publicKey, "host:443"))
    }
}