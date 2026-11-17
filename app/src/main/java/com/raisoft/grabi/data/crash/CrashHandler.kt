package com.raisoft.grabi.data.crash

import android.content.Context
import android.util.Log
import java.io.File

object CrashHandler {

    fun install(context: Context) {
        val crashDir = File(context.filesDir, "crashes")
        val default = Thread.getDefaultUncaughtExceptionHandler()
        val handler = Thread.UncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
            runCatching {
                crashDir.mkdirs()
                val report = buildString {
                    appendLine("timestamp=${System.currentTimeMillis()}")
                    appendLine("thread=${thread.name}")
                    appendLine("app=${context.packageName}")
                    appendLine(Log.getStackTraceString(throwable))
                }
                val target = File(crashDir, "crash-last.txt")
                val temporary = File(crashDir, "crash-last.tmp")
                temporary.writeText(report)
                temporary.renameTo(target)
            }
            default?.uncaughtException(thread, throwable)
        }
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    fun lastCrashTimestamp(context: Context): Long? = parseTimestamp(
        runCatching { File(context.filesDir, "crashes/crash-last.txt").readText() }.getOrNull(),
    )

    fun parseTimestamp(text: String?): Long? {
        val firstLine = text?.lineSequence()?.firstOrNull()?.trim() ?: return null
        return firstLine.substringAfter('=', firstLine).trim().toLongOrNull()
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, "crashes/crash-last.txt").delete() }
    }
}