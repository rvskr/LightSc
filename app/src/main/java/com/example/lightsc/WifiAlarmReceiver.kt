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
import androidx.core.content.ContextCompat

class WifiAlarmReceiver : BroadcastReceiver() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
        val requiredCount = intent.getIntExtra("requiredCount", 0)
        val interval = intent.getLongExtra("interval", 0)
        handleWifiScanResults(context, requiredCount, interval)
    }

    private fun handleWifiScanResults(context: Context, requiredCount: Int, interval: Long) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.startScan()

            Handler(Looper.getMainLooper()).postDelayed({
                val wifiList = wifiManager.scanResults
                logAndNotify("Number of Wi-Fi networks found: ${wifiList.size}")
                logAndNotify("Required Wi-Fi count: $requiredCount")

                if (wifiList.size >= requiredCount) {
                    logAndNotify("Number of Wi-Fi networks found is sufficient for playback")
                    playSound(context)
                } else {
                    logAndNotify("Number of Wi-Fi networks found is less than required count")
                }

                scheduleNextAlarm(context, requiredCount, interval)
            }, SCAN_RESULTS_DELAY_MS)
        } else {
            logAndNotify("Missing ACCESS_FINE_LOCATION permission")
        }
    }

    private fun playSound(context: Context) {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.sound_file).apply {
                setOnCompletionListener {
                    logAndNotify("Sound playback completed")
                }
                start()
            }
        } catch (e: Exception) {
            logAndNotify("Error playing sound: ${e.message}")
        }
    }

    private fun scheduleNextAlarm(context: Context, requiredCount: Int, interval: Long) {
        val alarmIntent = Intent(context, WifiAlarmReceiver::class.java).apply {
            putExtra("requiredCount", requiredCount)
            putExtra("interval", interval)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + interval

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun logAndNotify(message: String) {
        Log.d(TAG, message)
        TelegramManager.sendTelegramMessage(message)
    }

    companion object {
        private const val TAG = "WifiAlarmReceiver"
        private const val SCAN_RESULTS_DELAY_MS = 3000L
    }
}
