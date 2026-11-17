package com.raisoft.grabi.data.playback

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {

    private const val TAG = "GrabiDebug"
    private var file: File? = null
    private val ring = ArrayDeque<String>()
    private val stamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        file = File(context.filesDir, "debug-log.txt")
    }

    fun line(message: String) {
        val entry = synchronized(ring) {
            val stamped = "${stamp.format(Date())} $message"
            ring.addLast(stamped)
            while (ring.size > 400) ring.removeFirst()
            stamped
        }
        Log.d(TAG, message)
        runCatching {
            file?.let {
                if (it.length() > 512_000) it.writeText(read() + "\n") else it.appendText(entry + "\n")
            }
        }
    }

    fun read(): String = synchronized(ring) { ring.joinToString("\n") }
}