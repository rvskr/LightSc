// TelegramManager.kt
package com.example.lightsc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val telegramBotToken = "6923582894:AAHuYGsJuKLsKCcVccnN8kbsdRzC8vTiFVE"
private const val chatId = "558625598"

object TelegramManager {
    private var isTelegramEnabled = true

    fun toggleTelegramEnabled() {
        isTelegramEnabled = !isTelegramEnabled
    }

    fun isTelegramEnabled(): Boolean {
        return isTelegramEnabled
    }

    fun sendTelegramMessage(message: String) {
        if (isTelegramEnabled) {
            GlobalScope.launch(Dispatchers.IO) {
                val urlString = "https://api.telegram.org/bot$telegramBotToken/sendMessage"
                val query = String.format("chat_id=%s&text=%s",
                    URLEncoder.encode(chatId, "UTF-8"),
                    URLEncoder.encode(message, "UTF-8"))
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(query)
                outputStream.flush()
                outputStream.close()
                val responseCode = connection.responseCode
                // Обработка ответа, если нужно
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("Message sent successfully")
                } else {
                    println("Failed to send message. Response code: $responseCode")
                }
                connection.disconnect()
            }
        }
    }
}
