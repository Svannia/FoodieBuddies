package com.example.foodiebuddy.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.foodiebuddy.BuildConfig

object TelegramBot {
    private const val BOT_TOKEN = BuildConfig.TELEGRAM_BOT_TOKEN
    private const val CHAT_ID = BuildConfig.TELEGRAM_CHAT_ID

    suspend fun sendBugReport(username: String, message: String, logFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val boundary = "----FoodieBuddiesBoundary${System.currentTimeMillis()}"
                val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendDocument")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = connection.outputStream
                writeMultipart(outputStream, boundary, username, message, logFile)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                Timber.tag("Error").d("Failed to send bug report with error:\n$e")
                false
            }
        }
    }

    private fun writeMultipart(output: OutputStream, boundary: String, username: String, message: String, logFile: File) {
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"caption\"" + lineEnd +
                    lineEnd + "From $username:\n$message" + lineEnd).toByteArray()
        )

        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"chat_id\"" + lineEnd +
                    lineEnd + CHAT_ID + lineEnd).toByteArray()
        )

        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"document\"; filename=\"log.txt\"" + lineEnd +
                    "Content-Type: text/plain" + lineEnd +
                    lineEnd).toByteArray()
        )

        logFile.inputStream().copyTo(output)
        output.write((lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).toByteArray())
    }
}