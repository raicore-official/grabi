package com.raisoft.grabi.domain.vpn

import java.security.MessageDigest

object ConfigKeys {
    fun of(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}