package com.raisoft.grabi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamOptionMapperTest {

    private val video1080 = StreamOptionMapper.RawVideo(
        url = "https://cdn/v1080",
        resolution = "1080p",
        videoOnly = true,
        mimeType = "video/mp4",
    )
    private val video360 = StreamOptionMapper.RawVideo(
        url = "https://cdn/v360",
        resolution = "360p",
        videoOnly = false,
        mimeType = "video/mp4",
    )
    private val audioLow = StreamOptionMapper.RawAudio(
        url = "https://cdn/a64",
        bitrate = 64,
        mimeType = "audio/webm",
    )
    private val audioHigh = StreamOptionMapper.RawAudio(
        url = "https://cdn/a160",
        bitrate = 160,
        mimeType = "audio/mp4",
    )

    @Test
    fun videoOptionsSortedByHeightDescending() {
        val options = StreamOptionMapper.videoOptions(listOf(video360, video1080), emptyList())
        assertEquals(listOf(1080, 360), options.map { it.height })
    }

    @Test
    fun videoOnlyStreamsGetBestAudioAttached() {
        val options = StreamOptionMapper.videoOptions(listOf(video1080), listOf(audioLow, audioHigh))
        assertEquals("https://cdn/a160", options.single().audioUrl)
    }

    @Test
    fun progressiveStreamsHaveNoSeparateAudio() {
        val options = StreamOptionMapper.videoOptions(listOf(video360), listOf(audioHigh))
        assertNull(options.single().audioUrl)
    }

    @Test
    fun videoOnlyWithoutAudioTracksStillPlayable() {
        val options = StreamOptionMapper.videoOptions(listOf(video1080), emptyList())
        assertNull(options.single().audioUrl)
    }

    @Test
    fun blankResolutionGetsFallbackLabel() {
        val raw = video1080.copy(resolution = "")
        val option = StreamOptionMapper.videoOptions(listOf(raw), emptyList()).single()
        assertEquals("Video 1", option.label)
        assertEquals(0, option.height)
    }

    @Test
    fun audioOptionsSortedByBitrateWithCodecLabels() {
        val options = StreamOptionMapper.audioOptions(listOf(audioLow, audioHigh))
        assertEquals("https://cdn/a160", options[0].audioUrl)
        assertEquals("https://cdn/a64", options[1].audioUrl)
        assertEquals("mp4 · 160 kbps", options[0].label)
        assertEquals("webm · 64 kbps", options[1].label)
        assertTrue(options.all { it.videoUrl == null })
    }

    @Test
    fun emptyInputsProduceEmptyOptions() {
        assertTrue(StreamOptionMapper.videoOptions(emptyList(), emptyList()).isEmpty())
        assertTrue(StreamOptionMapper.audioOptions(emptyList()).isEmpty())
    }
}