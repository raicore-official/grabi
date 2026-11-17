package com.raisoft.grabi.domain.vpn

enum class ExpiryStatus { UNKNOWN, VALID, EXPIRING_SOON, EXPIRED, INVALID }

data class ExpiryRecord(
    val configKey: String,
    val generatedAt: Long,
    val expiresAt: Long?,
    val providerExpiryPolicy: String,
    val expirySource: String,
    val lastCheckedAt: Long,
    val invalid: Boolean = false,
)

object ExpiryCalculator {
    const val EXPIRING_SOON_MS = 24L * 3_600_000
    const val RENEW_LEAD_MS = 6L * 3_600_000
    const val CLOCK_SKEW_MS = 5L * 60_000

    fun status(record: ExpiryRecord?, now: Long): ExpiryStatus {
        if (record == null) return ExpiryStatus.UNKNOWN
        if (record.invalid) return ExpiryStatus.INVALID
        val expiresAt = record.expiresAt
        if (expiresAt == null || expiresAt <= 0L) return ExpiryStatus.UNKNOWN
        if (now < record.generatedAt - CLOCK_SKEW_MS) return ExpiryStatus.UNKNOWN
        if (now >= expiresAt) return ExpiryStatus.EXPIRED
        if (expiresAt - now <= EXPIRING_SOON_MS) return ExpiryStatus.EXPIRING_SOON
        return ExpiryStatus.VALID
    }

    fun remainingMs(record: ExpiryRecord?, now: Long): Long? {
        val expiresAt = record?.expiresAt ?: return null
        if (expiresAt <= 0L) return null
        return (expiresAt - now).coerceAtLeast(0L)
    }

    fun nextRenewalAt(record: ExpiryRecord?, now: Long): Long? {
        val expiresAt = record?.expiresAt ?: return null
        if (expiresAt <= 0L) return null
        return (expiresAt - RENEW_LEAD_MS).coerceAtLeast(now)
    }

    fun merge(existing: ExpiryRecord?, incoming: ExpiryRecord): ExpiryRecord {
        if (existing == null) return incoming
        return existing.copy(
            expiresAt = existing.expiresAt ?: incoming.expiresAt,
            providerExpiryPolicy = existing.providerExpiryPolicy.ifBlank { incoming.providerExpiryPolicy },
            expirySource = if (existing.expiresAt != null) existing.expirySource else incoming.expirySource,
            lastCheckedAt = incoming.lastCheckedAt,
        )
    }
}