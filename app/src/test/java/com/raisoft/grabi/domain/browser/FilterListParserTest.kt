package com.raisoft.grabi.domain.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListParserTest {

    private fun rules(vararg lines: String): FilterRules =
        FilterListParser.parse(lines.asSequence())

    private fun blocked(rules: FilterRules, url: String, host: String, path: String = "/"): Boolean =
        rules.isBlocked(url, host, path)

    @Test
    fun domainRuleBlocksSubdomains() {
        val r = rules("||ads.example.com^")
        assertTrue(blocked(r, "https://ads.example.com/banner.js", "ads.example.com"))
        assertTrue(blocked(r, "https://sub.ads.example.com/x", "sub.ads.example.com"))
        assertFalse(blocked(r, "https://example.com/", "example.com"))
    }

    @Test
    fun exceptionRuleWins() {
        val r = rules("||ads.example.com^", "@@||ads.example.com^")
        assertFalse(blocked(r, "https://ads.example.com/banner.js", "ads.example.com"))
    }

    @Test
    fun pathRuleBlocksMatchingPathsOnly() {
        val r = rules("||cdn.example.com/ads/")
        assertTrue(blocked(r, "https://cdn.example.com/ads/a.js", "cdn.example.com", "/ads/a.js"))
        assertFalse(blocked(r, "https://cdn.example.com/app.js", "cdn.example.com", "/app.js"))
    }

    @Test
    fun genericSubstringPatternBlocks() {
        val r = rules("/banner_ad/")
        assertTrue(blocked(r, "https://any.site.net/banner_ad/x.gif", "any.site.net", "/banner_ad/x.gif"))
        assertFalse(blocked(r, "https://any.site.net/logo.png", "any.site.net", "/logo.png"))
    }

    @Test
    fun commentsAndCosmeticRulesAreSkipped() {
        val r = rules(
            "! comment",
            "[Adblock Plus 2.0]",
            "example.com##.ad-banner",
            "example.com#@#.ad",
            "",
        )
        assertFalse(blocked(r, "https://example.com/", "example.com"))
    }

    @Test
    fun optionRulesAreSkippedToAvoidOverblocking() {
        val r = rules("||example.com^$third-party", "||tracker.net^$domain=foo.com")
        assertFalse(blocked(r, "https://example.com/", "example.com"))
        assertFalse(blocked(r, "https://tracker.net/", "tracker.net"))
    }

    @Test
    fun wildcardRulesAreSkipped() {
        val r = rules("||ads.*.com^", "||example.com^*")
        assertFalse(blocked(r, "https://ads.foo.com/", "ads.foo.com"))
    }

    @Test
    fun emptyRulesBlockNothing() {
        assertFalse(blocked(FilterRules.EMPTY, "https://anything.com/", "anything.com"))
    }
}