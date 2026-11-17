package com.raisoft.grabi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactNumberTest {

    @Test
    fun smallNumbersStayExact() {
        assertEquals("0", CompactNumber.format(0))
        assertEquals("999", CompactNumber.format(999))
    }

    @Test
    fun thousandsUseOneDecimalBelowTen() {
        assertEquals("1.5K", CompactNumber.format(1_500))
        assertEquals("23K", CompactNumber.format(23_000))
        assertEquals("240K", CompactNumber.format(240_400))
    }

    @Test
    fun millionsScale() {
        assertEquals("1.2M", CompactNumber.format(1_200_000))
        assertEquals("64M", CompactNumber.format(64_000_000))
    }
}