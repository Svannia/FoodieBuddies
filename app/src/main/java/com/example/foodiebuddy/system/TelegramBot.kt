package com.example.foodiebuddy.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramBot {
    private const val BOT_TOKEN = "7932493120:AAGlnyxtGQ2BOu6Gs17O2z6Zym1PRrMfZSc"
    private const val CHAT_ID = "1357658600"

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val urlString = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage?chat_id=$CHAT_ID&text=$encodedMessage"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}