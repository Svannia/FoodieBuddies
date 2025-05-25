package com.example.foodiebuddy.errors

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This class overwrites the use of Log to use Timber instead, which allows to write a file with all app logs.
 *
 * @property logFile: File to which the Timber logs are written
 * @property maxLines: max number of lines in the logs file. When the limit is reached, older logs are deleted to leave space for new logs
 */
class FileLoggingTree(
    private val logFile: File,
    private val maxLines: Int = 2000
) : Timber.DebugTree() {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logMessage = "[${dateFormat.format(Date())}] ${tag ?: "General"}: $message\n"
        try {
            logFile.appendText(logMessage)
            truncateIfTooLong()
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "Failed to write log", e)
        }
    }


    private fun truncateIfTooLong() {
        val lines = logFile.readLines()
        if (lines.size <= maxLines) return

        val trimmedLines = lines.takeLast(maxLines)
        logFile.writeText(trimmedLines.joinToString("\n") + "\n")
    }
}