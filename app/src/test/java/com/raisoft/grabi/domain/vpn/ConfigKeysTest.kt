package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigKeysTest {

    @Test
    fun keyIsSha256Hex() {
        val key = ConfigKeys.of("config")
        assertEquals(64, key.length)
        assertTrue(key.all { it in "0123456789abcdef" })
    }

    @Test
    fun surroundingWhitespaceIgnored() {
        assertEquals(ConfigKeys.of("config"), ConfigKeys.of("  config\n"))
    }

    @Test
    fun differentTextDifferentKey() {
        assertNotEquals(ConfigKeys.of("config-a"), ConfigKeys.of("config-b"))
    }

    @Test
    fun duplicateDetectionViaValidatorKey() {
        val a = ConfigValidator.duplicateKey("pub", "host:443")
        val b = ConfigValidator.duplicateKey("pub", "host:443")
        val c = ConfigValidator.duplicateKey("pub", "host:80")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}