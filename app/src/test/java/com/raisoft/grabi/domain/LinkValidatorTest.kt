package com.raisoft.grabi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorTest {

    @Test
    fun acceptsHttpsUrl() {
        assertTrue(LinkValidator.isValidUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun acceptsHttpUrl() {
        assertTrue(LinkValidator.isValidUrl("http://vimeo.com/22439234"))
    }

    @Test
    fun acceptsUrlWithSurroundingWhitespace() {
        assertTrue(LinkValidator.isValidUrl("  https://youtu.be/abc123  "))
    }

    @Test
    fun rejectsPlainText() {
        assertFalse(LinkValidator.isValidUrl("not a link"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertFalse(LinkValidator.isValidUrl("   "))
    }

    @Test
    fun rejectsNonHttpSchemes() {
        assertFalse(LinkValidator.isValidUrl("ftp://example.com/file"))
        assertFalse(LinkValidator.isValidUrl("javascript:alert(1)"))
    }

    @Test
    fun rejectsUrlWithSpaces() {
        assertFalse(LinkValidator.isValidUrl("https://example.com/some path"))
    }

    @Test
    fun extractsUrlFromShareText() {
        assertEquals(
            "https://youtu.be/abc123",
            LinkValidator.extractUrl("Watch this: https://youtu.be/abc123"),
        )
    }

    @Test
    fun trimsTrailingPunctuationFromExtractedUrl() {
        assertEquals(
            "https://youtu.be/abc123",
            LinkValidator.extractUrl("Check https://youtu.be/abc123."),
        )
    }

    @Test
    fun extractReturnsNullWhenNoUrl() {
        assertNull(LinkValidator.extractUrl("just some words"))
    }
}