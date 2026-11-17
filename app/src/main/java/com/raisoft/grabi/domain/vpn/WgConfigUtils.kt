package com.raisoft.grabi.domain.vpn

import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.StringReader

object WgConfigUtils {

    fun parse(text: String): Config = Config.parse(BufferedReader(StringReader(text)))

    fun isUsableClient(config: Config): Boolean =
        config.peers.any { it.endpoint.isPresent }

    fun endpointLabel(config: Config): String? =
        config.peers.firstNotNullOfOrNull { it.endpoint.orElse(null) }
            ?.let { "${it.host}:${it.port}" }

    fun publicKeyOf(config: Config): String =
        config.getInterface().keyPair.publicKey.toBase64()

    fun withExcludedApps(config: Config, packages: Set<String>): Config {
        if (packages.isEmpty() && config.getInterface().excludedApplications.isEmpty()) return config
        val lines = config.toWgQuickString().lines().filterNot {
            val trimmed = it.trim()
            trimmed.startsWith("ExcludedApplications") || trimmed.startsWith("IncludedApplications")
        }
        val out = ArrayList<String>(lines.size + 1)
        for (line in lines) {
            out += line
            if (line.trim() == "[Interface]" && packages.isNotEmpty()) {
                out += "ExcludedApplications = ${packages.joinToString(", ")}"
            }
        }
        return parse(out.joinToString("\n"))
    }

    fun sanitizeName(raw: String): String {
        val cleaned = raw
            .replace(Regex("[^a-zA-Z0-9_=+.-]"), "-")
            .trim('-')
            .take(Tunnel.NAME_MAX_LENGTH)
        return cleaned.ifEmpty { "grabi" }
    }
}