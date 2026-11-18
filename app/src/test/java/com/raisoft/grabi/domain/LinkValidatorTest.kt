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

    @Test
    fun acceptsMagnetWithInfoHash() {
        assertTrue(
            LinkValidator.isValidUrl("magnet:?xt=urn:btih:af0d9aa01a9ae123a73802cfa58ccaf355eb19f1&dn=Test"),
        )
    }

    @Test
    fun rejectsMagnetWithoutInfoHash() {
        assertFalse(LinkValidator.isValidUrl("magnet:?dn=Test"))
    }

    @Test
    fun extractsMagnetFromShareText() {
        assertEquals(
            "magnet:?xt=urn:btih:abc123",
            LinkValidator.extractUrl("Grab this magnet:?xt=urn:btih:abc123 now"),
        )
    }

    @Test
    fun detectsTorrentLinks() {
        assertTrue(LinkValidator.isTorrent("magnet:?xt=urn:btih:abc123"))
        assertTrue(LinkValidator.isTorrent("https://example.com/file.torrent"))
        assertFalse(LinkValidator.isTorrent("https://example.com/file.mp4"))
    }

    @Test
    fun torrentTitleFromMagnetDisplayName() {
        assertEquals("My File", LinkValidator.torrentTitle("magnet:?xt=urn:btih:abc&dn=My%20File"))
        assertEquals("Torrent", LinkValidator.torrentTitle("magnet:?xt=urn:btih:abc"))
    }

    @Test
    fun torrentTitleFromTorrentFileUrl() {
        assertEquals("ubuntu", LinkValidator.torrentTitle("https://example.com/ubuntu.torrent"))
    }
}