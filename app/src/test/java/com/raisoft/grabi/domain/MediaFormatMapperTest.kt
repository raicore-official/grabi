package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.FormatSpec
import com.raisoft.grabi.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFormatMapperTest {

    private fun spec(
        height: Int,
        vcodec: String? = "avc1",
        acodec: String? = "none",
        size: Long = 0,
    ) = FormatSpec("id-$height-$size", "mp4", height, vcodec, acodec, size)

    @Test
    fun heightsDeduplicatedAndSortedDescending() {
        val options = MediaFormatMapper.options(listOf(spec(360), spec(1080), spec(360), spec(720)))
        val video = options.filter { it.kind == MediaKind.VIDEO }
        assertEquals(listOf("1080p", "720p", "360p"), video.map { it.label })
    }

    @Test
    fun audioOnlySourceStillOffersAudioFormats() {
        val options = MediaFormatMapper.options(listOf(spec(0, vcodec = "none", acodec = "mp4a")))
        assertEquals(3, options.size)
        assertTrue(options.all { it.kind == MediaKind.AUDIO })
        assertEquals(listOf("mp3", "m4a", "opus"), options.map { it.audioFormat })
    }

    @Test
    fun emptyFormatsYieldAudioOnly() {
        assertEquals(3, MediaFormatMapper.options(emptyList()).size)
    }

    @Test
    fun audioOnlyFormatsExcludedFromVideoHeights() {
        val options = MediaFormatMapper.options(listOf(spec(1080), spec(0, vcodec = "none", acodec = "mp4a")))
        assertEquals(1, options.count { it.kind == MediaKind.VIDEO })
    }

    @Test
    fun videoSelectorMergesBestAudio() {
        val video = MediaFormatMapper.options(listOf(spec(720))).first()
        assertEquals("bestvideo[height<=720]+bestaudio/best[height<=720]", video.formatSelector)
        assertNull(video.audioFormat)
    }

    @Test
    fun sizeUsesLargestCandidate() {
        val options = MediaFormatMapper.options(listOf(spec(720, size = 100), spec(720, size = 500)))
        assertEquals(500L, options.first { it.kind == MediaKind.VIDEO }.sizeBytes)
    }

    @Test
    fun zeroSizeBecomesNull() {
        assertNull(MediaFormatMapper.options(listOf(spec(720))).first().sizeBytes)
    }

    @Test
    fun zeroHeightIgnored() {
        assertTrue(MediaFormatMapper.options(listOf(spec(0))).all { it.kind == MediaKind.AUDIO })
    }
}