package com.raisoft.grabi.data.stream

import com.raisoft.grabi.domain.model.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamCache @Inject constructor() {

    var ttlMs: Long = 90 * 60 * 1000L

    private data class Entry(val info: StreamInfo, val savedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun get(url: String, now: Long = System.currentTimeMillis()): StreamInfo? =
        entries[url]?.takeIf { now - it.savedAt < ttlMs }?.info

    fun put(url: String, info: StreamInfo, now: Long = System.currentTimeMillis()) {
        entries[url] = Entry(info, now)
    }

    fun invalidate(url: String) {
        entries.remove(url)
    }
}