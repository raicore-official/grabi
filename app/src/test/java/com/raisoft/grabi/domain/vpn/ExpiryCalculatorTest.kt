package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiryCalculatorTest {

    private val now = 1_000_000_000_000L
    private val hour = 3_600_000L

    private fun record(
        expiresAt: Long?,
        generatedAt: Long = now - 24 * hour,
        invalid: Boolean = false,
    ) = ExpiryRecord(
        configKey = "key",
        generatedAt = generatedAt,
        expiresAt = expiresAt,
        providerExpiryPolicy = "7d",
        expirySource = "provider",
        lastCheckedAt = now,
        invalid = invalid,
    )

    @Test
    fun nullRecordIsUnknown() {
        assertEquals(ExpiryStatus.UNKNOWN, ExpiryCalculator.status(null, now))
    }

    @Test
    fun invalidRecordIsInvalid() {
        assertEquals(ExpiryStatus.INVALID, ExpiryCalculator.status(record(now + hour, invalid = true), now))
    }

    @Test
    fun noExpiryIsUnknown() {
        assertEquals(ExpiryStatus.UNKNOWN, ExpiryCalculator.status(record(null), now))
        assertEquals(ExpiryStatus.UNKNOWN, ExpiryCalculator.status(record(0L), now))
    }

    @Test
    fun clockSkewIntoFutureIsUnknown() {
        val future = record(expiresAt = now + 48 * hour, generatedAt = now + hour)
        assertEquals(ExpiryStatus.UNKNOWN, ExpiryCalculator.status(future, now))
    }

    @Test
    fun smallClockSkewTolerated() {
        val slightlyFuture = record(expiresAt = now + 48 * hour, generatedAt = now + 60_000)
        assertEquals(ExpiryStatus.VALID, ExpiryCalculator.status(slightlyFuture, now))
    }

    @Test
    fun expiredAtBoundary() {
        assertEquals(ExpiryStatus.EXPIRED, ExpiryCalculator.status(record(now), now))
        assertEquals(ExpiryStatus.EXPIRED, ExpiryCalculator.status(record(now - 1), now))
    }

    @Test
    fun expiringSoonWithin24h() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryCalculator.status(record(now + 12 * hour), now))
        assertEquals(ExpiryStatus.EXPIRING_SOON, ExpiryCalculator.status(record(now + 24 * hour), now))
    }

    @Test
    fun validBeyond24h() {
        assertEquals(ExpiryStatus.VALID, ExpiryCalculator.status(record(now + 48 * hour), now))
    }

    @Test
    fun remainingClampedAtZero() {
        assertEquals(0L, ExpiryCalculator.remainingMs(record(now - hour), now))
        assertEquals(2 * hour, ExpiryCalculator.remainingMs(record(now + 2 * hour), now))
        assertNull(ExpiryCalculator.remainingMs(record(null), now))
    }

    @Test
    fun renewalLeadsExpiryBySixHours() {
        assertEquals(now + 42 * hour, ExpiryCalculator.nextRenewalAt(record(now + 48 * hour), now))
        assertEquals(now, ExpiryCalculator.nextRenewalAt(record(now + hour), now))
        assertNull(ExpiryCalculator.nextRenewalAt(record(null), now))
    }

    @Test
    fun mergeNeverResetsExistingExpiry() {
        val existing = record(expiresAt = now + 12 * hour)
        val incoming = record(expiresAt = now + 7 * 24 * hour).copy(lastCheckedAt = now + 5 * hour)
        val merged = ExpiryCalculator.merge(existing, incoming)
        assertEquals(existing.expiresAt, merged.expiresAt)
        assertEquals(existing.generatedAt, merged.generatedAt)
        assertEquals(incoming.lastCheckedAt, merged.lastCheckedAt)
    }

    @Test
    fun mergeFillsMissingExpiry() {
        val existing = record(expiresAt = null)
        val incoming = record(expiresAt = now + 48 * hour)
        assertEquals(incoming.expiresAt, ExpiryCalculator.merge(existing, incoming).expiresAt)
        assertEquals(incoming, ExpiryCalculator.merge(null, incoming))
    }
}