package com.locuspace

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TrackingService : Service() {

    private val channelId = "tracking_channel"
    private val notifId = 1

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundWithNotification()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Run tracking active")
            .setContentText("Tracking your run in the background")
            // ⚠️ This icon MUST exist in your project (res/drawable or mipmap)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(notifId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // We just stay alive; location is handled by your activity or here later
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
