package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnBookParserTest {

    private val page = """
        <html><body>
        <a href="/freevpn/wireguard-vpn/us16.vpnbook.com">US16</a>
        <a href="/freevpn/wireguard-vpn/de20.vpnbook.com">DE20</a>
        <a href="/freevpn/wireguard-vpn/uk205.vpnbook.com">UK205</a>
        <span>us16.vpnbook.com</span>
        <span>xx99.vpnbook.com</span>
        <script>var turnstileSiteKey = "0x4AAAAAACQ2rPOmG8WoEc0q";</script>
        </body></html>
    """.trimIndent()

    @Test
    fun parsesDistinctServersWithCountries() {
        val servers = VpnBookParser.parseServers(page)
        assertEquals(3, servers.size)
        assertEquals("us16.vpnbook.com", servers[0].hostname)
        assertEquals("US", servers[0].country)
        assertEquals("DE", servers[1].country)
        assertEquals("GB", servers[2].country)
    }

    @Test
    fun unknownCountryPrefixIgnored() {
        assertTrue(VpnBookParser.parseServers(page).none { it.id == "xx99" })
    }

    @Test
    fun emptyPageYieldsNoServers() {
        assertTrue(VpnBookParser.parseServers("<html></html>").isEmpty())
    }

    @Test
    fun detectsTurnstile() {
        assertTrue(VpnBookParser.requiresHumanVerification(page))
        assertTrue(VpnBookParser.requiresHumanVerification("src=https://challenges.cloudflare.com/turnstile"))
        assertFalse(VpnBookParser.requiresHumanVerification("<html>plain page</html>"))
    }

    @Test
    fun portsMatchPublishedSet() {
        assertEquals(listOf(443, 80, 123, 25018), VpnBookParser.PORTS)
    }
}