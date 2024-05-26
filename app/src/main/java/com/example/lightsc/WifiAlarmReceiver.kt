package com.example.lightsc

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WifiAlarmReceiver : BroadcastReceiver() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Wi-Fi Alarm received")

        val requiredCount = intent.getIntExtra("requiredCount", 0)
        val interval = intent.getLongExtra("interval", 0)

        // Сканируем доступные Wi-Fi сети и отправляем лог в Telegram
        handleWifiScanResults(context, requiredCount, interval)
    }

    private fun handleWifiScanResults(context: Context, requiredCount: Int, interval: Long) {
        // Проверяем наличие разрешения на доступ к информации о Wi-Fi сетях
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Получаем менеджер Wi-Fi
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Запускаем сканирование Wi-Fi сетей
            wifiManager.startScan()

            // Обработка результатов сканирования через задержку, чтобы дождаться завершения сканирования
            Handler(Looper.getMainLooper()).postDelayed({
                val wifiList = wifiManager.scanResults

                // Логируем количество доступных Wi-Fi сетей
                Log.d(TAG, "Number of Wi-Fi networks found: ${wifiList.size}")

                // Логируем требуемое количество Wi-Fi сетей
                Log.d(TAG, "Required Wi-Fi count: $requiredCount")

                // Отправляем лог в Telegram
                sendLogToTelegram(wifiList.size, requiredCount)

                // Проверяем условие перед воспроизведением звука
                if (wifiList.size >= requiredCount) {
                    Log.d(TAG, "Number of Wi-Fi networks found is sufficient for playback")
                    playSound(context)
                } else {
                    Log.d(TAG, "Number of Wi-Fi networks found is less than required count")
                }

                // Планируем следующий запуск будильника
                scheduleNextAlarm(context, requiredCount, interval)
            }, SCAN_RESULTS_DELAY_MS)
        } else {
            Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission")
        }
    }

    private fun playSound(context: Context) {
        try {
            val soundResource = R.raw.sound_file
            if (soundResource == 0) {
                Log.e(TAG, "Sound resource not found")
                return
            }

            mediaPlayer = MediaPlayer.create(context, soundResource)
            mediaPlayer?.setOnCompletionListener {
                Log.d(TAG, "Sound playback completed")
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}")
        }
    }

    private fun scheduleNextAlarm(context: Context, requiredCount: Int, interval: Long) {
        val alarmIntent = Intent(context, WifiAlarmReceiver::class.java).apply {
            putExtra("requiredCount", requiredCount)
            putExtra("interval", interval)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Получаем AlarmManager и устанавливаем сигнал будильника
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Устанавливаем время, через которое нужно запустить ресивер снова
        val triggerTime = System.currentTimeMillis() + interval

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun sendLogToTelegram(actualCount: Int, requiredCount: Int) {
        val message = "Number of Wi-Fi networks found: $actualCount\nRequired Wi-Fi count: $requiredCount"
        sendTelegramMessage(message)
    }

    companion object {
        private const val TAG = "WifiAlarmReceiver"
        private const val SCAN_RESULTS_DELAY_MS = 3000L // Задержка для получения результатов сканирования Wi-Fi
    }
}
