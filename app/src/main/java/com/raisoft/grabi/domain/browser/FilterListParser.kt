package com.raisoft.grabi.domain.browser

/**
 * Lightweight ABP-style network filter matcher. Rules with options ($third-party,
 * $domain=, …) and cosmetic rules are skipped on purpose: under-blocking is safe,
 * over-blocking breaks sites.
 */
class FilterRules(
    private val blockedDomains: Set<String>,
    private val allowedDomains: Set<String>,
    private val pathRules: Map<String, List<String>>,
    private val genericPatterns: List<String>,
) {

    fun isBlocked(url: String, host: String, path: String): Boolean {
        if (matchesSuffix(host, allowedDomains)) return false
        if (matchesSuffix(host, blockedDomains)) return true
        var domain = host
        while (true) {
            pathRules[domain]?.let { prefixes ->
                if (prefixes.any { path.startsWith(it) }) return true
            }
            val next = domain.substringAfter('.', "")
            if (next.isEmpty() || next == domain) break
            domain = next
        }
        return genericPatterns.any { url.contains(it) }
    }

    private fun matchesSuffix(host: String, domains: Set<String>): Boolean {
        var domain = host
        while (true) {
            if (domain in domains) return true
            val next = domain.substringAfter('.', "")
            if (next.isEmpty() || next == domain) return false
            domain = next
        }
    }

    companion object {
        val EMPTY = FilterRules(emptySet(), emptySet(), emptyMap(), emptyList())
    }
}

object FilterListParser {

    private const val MAX_GENERIC_PATTERNS = 3000

    fun parse(lines: Sequence<String>): FilterRules {
        val blocked = HashSet<String>()
        val allowed = HashSet<String>()
        val paths = HashMap<String, MutableList<String>>()
        val generic = ArrayList<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
            if (line.contains("##") || line.contains("#@#") || line.contains("#?#")) continue
            var rule = line
            var exception = false
            if (rule.startsWith("@@")) {
                exception = true
                rule = rule.substring(2)
            }
            if (rule.contains('$')) continue
            if (!rule.startsWith("||")) {
                if (!exception &&
                    rule.length >= 4 &&
                    rule.none { it in "*^|\\" } &&
                    generic.size < MAX_GENERIC_PATTERNS
                ) {
                    generic.add(rule)
                }
                continue
            }
            val body = rule.substring(2)
            val end = body.indexOfFirst { it == '^' || it == '/' || it == '*' || it == '?' }
            val domain = (if (end < 0) body else body.substring(0, end)).lowercase()
            if (domain.isEmpty() || !domain.contains('.')) continue
            val rest = if (end < 0) "" else body.substring(end)
            when {
                rest.isEmpty() || rest == "^" || rest == "^|" ->
                    if (exception) allowed.add(domain) else blocked.add(domain)
                rest.startsWith("/") && !exception -> {
                    val pathEnd = rest.indexOfFirst { it == '^' || it == '*' }
                    val path = if (pathEnd < 0) rest else rest.substring(0, pathEnd)
                    if (path.length > 1) paths.getOrPut(domain) { ArrayList() }.add(path)
                }
            }
        }
        return FilterRules(blocked, allowed, paths, generic)
    }
}