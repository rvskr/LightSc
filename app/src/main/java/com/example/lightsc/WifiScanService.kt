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
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class WifiScanService : Service() {

    private lateinit var wifiManager: WifiManager
    private var isScanning = false
    private var scanInterval: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "WifiScanServiceChannel"
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scanInterval = intent?.getLongExtra("scanInterval", 0) ?: 0
        logAndNotify("Received scan interval: $scanInterval milliseconds")

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WifiScanService::WakeLock")
        wakeLock.acquire(scanInterval + 10000) // Принимаем WakeLock на время выполнения сканирования

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.scanning_wifi))
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
        startWifiScan()

        return START_NOT_STICKY
    }

    private fun startWifiScan() {
        if (!isScanning) {
            isScanning = true
            handler.post(scanRunnable)
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            wifiManager.startScan()
            logAndNotify("Wi-Fi scan started with interval $scanInterval")
            handler.postDelayed(this, scanInterval)
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

    private fun logAndNotify(message: String) {
        Log.d(TAG, message)
        TelegramManager.sendTelegramMessage(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(scanRunnable)
        wakeLock.release()
        logAndNotify("WifiScanService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "WifiScanService"
    }
}
