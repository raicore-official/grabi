package com.raisoft.grabi.data.crash

import android.content.Context
import android.util.Log
import java.io.File

object CrashHandler {

    fun install(context: Context) {
        val crashDir = File(context.filesDir, "crashes")
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crashDir.mkdirs()
                File(crashDir, "crash-last.txt")
                    .writeText("${System.currentTimeMillis()}\n${Log.getStackTraceString(throwable)}")
            }
            default?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrashTimestamp(context: Context): Long? = parseTimestamp(
        runCatching { File(context.filesDir, "crashes/crash-last.txt").readText() }.getOrNull(),
    )

    fun parseTimestamp(text: String?): Long? =
        text?.lineSequence()?.firstOrNull()?.trim()?.toLongOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, "crashes/crash-last.txt").delete() }
    }
}