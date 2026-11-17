package com.raisoft.grabi.domain.vpn

object ConfigValidator {

    sealed interface Result {
        data class Valid(val publicKey: String, val endpoint: String, val allowedIps: List<String>) : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(text: String): Result {
        if (text.isBlank()) return Result.Invalid("empty configuration")
        val lower = text.lowercase()
        if (!lower.contains("[interface]") || !lower.contains("[peer]")) {
            return Result.Invalid("missing [Interface] or [Peer] section")
        }
        val config = try {
            WgConfigUtils.parse(text)
        } catch (e: Exception) {
            return Result.Invalid("malformed WireGuard syntax")
        }
        val iface = config.getInterface()
        if (!isKey(iface.keyPair.privateKey.toBase64())) return Result.Invalid("invalid private key format")
        val interfacePublicKey = iface.keyPair.publicKey.toBase64()
        if (!isKey(interfacePublicKey)) return Result.Invalid("invalid public key format")
        if (iface.addresses.isEmpty()) return Result.Invalid("no interface address")
        val peer = config.peers.firstOrNull() ?: return Result.Invalid("no peer section")
        val publicKey = peer.publicKey.toBase64()
        if (!isKey(publicKey)) return Result.Invalid("invalid peer public key")
        val endpoint = peer.endpoint.orElse(null) ?: return Result.Invalid("peer has no endpoint")
        if (endpoint.port !in 1..65535) return Result.Invalid("invalid endpoint port")
        if (endpoint.host.isNullOrBlank()) return Result.Invalid("invalid endpoint host")
        if (peer.allowedIps.isEmpty()) return Result.Invalid("peer has no AllowedIPs")
        return Result.Valid(publicKey, "${endpoint.host}:${endpoint.port}", peer.allowedIps.map { it.toString() })
    }

    fun duplicateKey(publicKey: String, endpoint: String): String = "$publicKey|$endpoint"

    private fun isKey(base64: String): Boolean = try {
        java.util.Base64.getDecoder().decode(base64).size == 32
    } catch (e: IllegalArgumentException) {
        false
    }
}