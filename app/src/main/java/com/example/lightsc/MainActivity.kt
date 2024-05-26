package com.example.lightsc

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var intervalInput: EditText
    private lateinit var wifiCountInput: EditText
    private lateinit var startStopButton: Button
    private lateinit var disableTelegramButton: Button
    private var isAlarmRunning = false
    private lateinit var sharedPreferences: SharedPreferences
    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intervalInput = findViewById(R.id.interval_input)
        wifiCountInput = findViewById(R.id.wifi_count_input)
        startStopButton = findViewById(R.id.start_stop_button)
        disableTelegramButton = findViewById(R.id.disable_telegram_button)

        sharedPreferences = getSharedPreferences("com.example.lightsc", Context.MODE_PRIVATE)

        startStopButton.setOnClickListener {
            if (isAlarmRunning) {
                stopAlarm()
            } else {
                startAlarm()
            }
        }

        disableTelegramButton.setOnClickListener {
            toggleTelegramFunctionality()
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        Log.d(TAG, "Requesting permissions...")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun startAlarm() {
        val interval = intervalInput.text.toString().toLong() * 1000
        val requiredCount = wifiCountInput.text.toString().toInt()

        val alarmIntent = Intent(this, WifiAlarmReceiver::class.java).apply {
            putExtra("requiredCount", requiredCount)
            putExtra("interval", interval)
        }

        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent)

        logAndToast("Alarm started with interval $interval ms and required Wi-Fi count $requiredCount")
        startStopButton.text = "Stop"
        isAlarmRunning = true

        startWifiScanService(interval)
    }

    private fun stopAlarm() {
        sharedPreferences.edit().remove("wifiCount").apply()

        val alarmIntent = Intent(this, WifiAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        logAndToast("Alarm stopped")
        startStopButton.text = "Start"
        isAlarmRunning = false

        stopWifiScanService()
    }

    private fun startWifiScanService(interval: Long) {
        val serviceIntent = Intent(this, WifiScanService::class.java).apply {
            putExtra("scanInterval", interval)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopWifiScanService() {
        serviceIntent?.let {
            stopService(it)
            serviceIntent = null
        }
    }

    private fun toggleTelegramFunctionality() {
        TelegramManager.toggleTelegramEnabled()
        disableTelegramButton.text = if (TelegramManager.isTelegramEnabled()) "Disable Telegram" else "Enable Telegram"
    }

    private fun logAndToast(message: String) {
        Log.d(TAG, message)
        TelegramManager.sendTelegramMessage(message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
    }
}
