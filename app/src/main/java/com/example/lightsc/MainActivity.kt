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
    private var isAlarmRunning = false
    private lateinit var sharedPreferences: SharedPreferences
    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intervalInput = findViewById(R.id.interval_input)
        wifiCountInput = findViewById(R.id.wifi_count_input)
        startStopButton = findViewById(R.id.start_stop_button)

        sharedPreferences = getSharedPreferences("com.example.lightsc", Context.MODE_PRIVATE)

        startStopButton.setOnClickListener {
            if (isAlarmRunning) {
                stopAlarm()
            } else {
                startAlarm()
            }
        }

        // Запрашиваем разрешения при запуске приложения
        requestPermissions()
    }

    private fun requestPermissions() {
        Log.d(TAG, "Requesting permissions...")
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun startAlarm() {
        val interval = intervalInput.text.toString().toLong() * 1000 // Преобразуем секунды в миллисекунды
        val requiredCount = wifiCountInput.text.toString().toInt() // Получаем требуемое количество Wi-Fi сетей

        // Создаем новый Intent для передачи данных в BroadcastReceiver
        val alarmIntent = Intent(this, WifiAlarmReceiver::class.java).apply {
            putExtra("requiredCount", requiredCount)
            putExtra("interval", interval)
        }

        // Остальной код оставляем без изменений
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent?.let {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, it)
        }

        val logMessage = "Alarm started with interval $interval ms and required Wi-Fi count $requiredCount"
        Log.d(TAG, logMessage)
        // Отправляем лог в Телеграм
        sendTelegramMessage(logMessage)

        startStopButton.text = "Stop"
        isAlarmRunning = true

        // Запускаем фоновый сервис
        startWifiScanService()
    }

    private fun stopAlarm() {
        // Удаляем значение из SharedPreferences при остановке будильника
        sharedPreferences.edit().remove("wifiCount").apply()

        val alarmIntent = Intent(this, WifiAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val logMessage = "Alarm stopped"
        Log.d(TAG, logMessage)
        // Отправляем лог в Телеграм
        sendTelegramMessage(logMessage)

        Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show()
        startStopButton.text = "Start"
        isAlarmRunning = false

        // Останавливаем фоновый сервис
        stopWifiScanService()
    }

    private fun startWifiScanService() {
        serviceIntent = Intent(this, WifiScanService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent!!)
    }

    private fun stopWifiScanService() {
        serviceIntent?.let {
            stopService(it)
            serviceIntent = null
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
    }
}
