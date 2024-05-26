package com.example.lightsc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class WifiScanService : Service() {

    private lateinit var wifiManager: WifiManager
    private var isScanning = false
    private var scanInterval: Long = 60000 // 60 секунд
    private val CHANNEL_ID = "WifiScanServiceChannel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WifiScanService onStartCommand() called")

        // Получаем менеджер Wi-Fi
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Создаем уведомление для сервиса
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.scanning_wifi))
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        // Запускаем сервис в foreground режиме
        startForeground(1, notification)

        // Запускаем сканирование Wi-Fi сетей
        startWifiScan()

        // Возвращаем флаг, чтобы сервис не был автоматически перезапущен после прекращения работы
        return START_NOT_STICKY
    }

    private fun startWifiScan() {
        if (!isScanning) {
            isScanning = true
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                wifiManager.startScan()
                // Повторяем сканирование через определенный интервал времени
                handler.postDelayed({ startWifiScan() }, scanInterval)
            }, 1000) // Небольшая задержка перед началом сканирования

            // Отправляем лог в Telegram
            val logs = "WifiScanService started"
            TelegramManager.sendTelegramMessage(logs)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WifiScanService onDestroy() called")

        // Отправляем лог в Telegram при завершении сервиса
        val logs = "WifiScanService stopped"
        TelegramManager.sendTelegramMessage(logs)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "WifiScanService"
    }
}
