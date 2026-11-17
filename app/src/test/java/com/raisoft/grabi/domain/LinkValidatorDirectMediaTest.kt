package com.raisoft.grabi.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorDirectMediaTest {

    @Test
    fun plainMp4LinkIsDirect() {
        assertTrue(LinkValidator.isDirectMediaUrl("https://cdn.example.com/video/clip.mp4"))
    }

    @Test
    fun queryStringDoesNotHideExtension() {
        assertTrue(LinkValidator.isDirectMediaUrl("https://cdn.example.com/a.mp4?token=abc&x=1"))
    }

    @Test
    fun uppercaseExtensionAccepted() {
        assertTrue(LinkValidator.isDirectMediaUrl("https://cdn.example.com/song.MP3"))
    }

    @Test
    fun audioExtensionsAccepted() {
        assertTrue(LinkValidator.isDirectMediaUrl("https://x.example/a.m4a"))
        assertTrue(LinkValidator.isDirectMediaUrl("https://x.example/a.opus"))
        assertTrue(LinkValidator.isDirectMediaUrl("https://x.example/a.flac"))
    }

    @Test
    fun youtubeWatchLinkIsNotDirect() {
        assertFalse(LinkValidator.isDirectMediaUrl("https://www.youtube.com/watch?v=abc123"))
    }

    @Test
    fun pathWithoutExtensionIsNotDirect() {
        assertFalse(LinkValidator.isDirectMediaUrl("https://example.com/media/clip"))
    }

    @Test
    fun malformedUrlIsNotDirect() {
        assertFalse(LinkValidator.isDirectMediaUrl("not a url"))
    }
}