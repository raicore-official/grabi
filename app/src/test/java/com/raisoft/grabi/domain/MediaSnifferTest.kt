package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSnifferTest {

    @Test
    fun detectsVideoByExtension() {
        assertEquals(MediaKind.VIDEO, MediaSniffer.classify("https://cdn.example.com/clip.mp4", null))
        assertEquals(MediaKind.VIDEO, MediaSniffer.classify("https://cdn.example.com/live/stream.m3u8", null))
    }

    @Test
    fun detectsAudioByExtension() {
        assertEquals(MediaKind.AUDIO, MediaSniffer.classify("https://cdn.example.com/song.mp3", null))
        assertEquals(MediaKind.AUDIO, MediaSniffer.classify("https://cdn.example.com/track.m4a?token=1", null))
    }

    @Test
    fun detectsImageByExtension() {
        assertEquals(MediaKind.IMAGE, MediaSniffer.classify("https://cdn.example.com/photo.JPG", null))
        assertEquals(MediaKind.IMAGE, MediaSniffer.classify("https://cdn.example.com/a.webp", null))
    }

    @Test
    fun mimeTypeWinsOverExtension() {
        assertEquals(MediaKind.VIDEO, MediaSniffer.classify("https://cdn.example.com/file.bin", "video/mp4"))
        assertEquals(MediaKind.IMAGE, MediaSniffer.classify("https://cdn.example.com/file", "image/png"))
        assertEquals(
            MediaKind.VIDEO,
            MediaSniffer.classify("https://cdn.example.com/playlist", "application/vnd.apple.mpegurl"),
        )
    }

    @Test
    fun ignoresNonMediaUrls() {
        assertNull(MediaSniffer.classify("https://example.com/page.html", null))
        assertNull(MediaSniffer.classify("https://example.com/api/data.json", null))
        assertNull(MediaSniffer.classify("https://example.com/", null))
    }

    @Test
    fun ignoresBlobAndDataUrls() {
        assertNull(MediaSniffer.classify("blob:https://example.com/abc", null))
        assertNull(MediaSniffer.classify("data:image/png;base64,AAAA", null))
    }

    @Test
    fun malformedUrlsReturnNull() {
        assertNull(MediaSniffer.classify("not a url", null))
    }
}