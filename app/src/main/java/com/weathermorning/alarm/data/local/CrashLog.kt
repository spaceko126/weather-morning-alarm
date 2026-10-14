package com.weathermorning.alarm.data.local

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Writes the last few uncaught exceptions to a file inside the app's own
 * storage.
 *
 * This is not a replacement for a crash-reporting service: nothing is uploaded,
 * so it only helps when the phone is in reach. It exists because a sideloaded
 * alarm that dies overnight otherwise leaves nothing at all to look at.
 */
object CrashLog {
    private const val FILE_NAME = "crash-log.txt"
    private const val MAX_BYTES = 64 * 1024

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { append(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else ""
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE_NAME).delete()
    }

    private fun append(context: Context, thread: Thread, throwable: Throwable) {
        val file = File(context.filesDir, FILE_NAME)
        // Keep the file bounded; the newest crash is the one worth reading.
        if (file.exists() && file.length() > MAX_BYTES) file.delete()

        val stack = StringWriter().also { writer ->
            PrintWriter(writer).use(throwable::printStackTrace)
        }
        file.appendText(
            buildString {
                appendLine("---- ${Instant.now()} on ${thread.name} ----")
                appendLine(stack.toString())
            },
        )
    }
}
