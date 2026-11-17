package com.raisoft.grabi.data.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrashHandlerTest {

    @Test
    fun parsesTimestampFromFirstLine() {
        assertEquals(1731830400000L, CrashHandler.parseTimestamp("1731830400000\njava.lang.RuntimeException: boom\n\tat Foo.bar"))
    }

    @Test
    fun toleratesWhitespace() {
        assertEquals(42L, CrashHandler.parseTimestamp("  42  \ntrace"))
    }

    @Test
    fun rejectsGarbageAndEmpty() {
        assertNull(CrashHandler.parseTimestamp(null))
        assertNull(CrashHandler.parseTimestamp(""))
        assertNull(CrashHandler.parseTimestamp("not-a-number\ntrace"))
    }
}