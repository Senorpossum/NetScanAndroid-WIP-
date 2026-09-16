package com.example.networkscanner.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class SentinelNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Network Sentinel Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification indicating the sentinel is active."
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alerts for MitM attacks and rogue devices."
            }

            notificationManager.createNotificationChannel(foregroundChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun getForegroundNotification(content: String = "Monitoring network..."): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setContentTitle("Network Sentinel Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_secure) // Scaffold default icon
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun updateForegroundNotification(content: String) {
        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, getForegroundNotification(content))
    }

    fun dispatchArpSpoofAlert(details: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("CRITICAL: ARP Spoofing Detected!")
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun dispatchRogueDeviceAlert(macAddress: String, vendor: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("New Unknown Device Joined")
            .setContentText("MAC: $macAddress ($vendor)")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID_FOREGROUND = "sentinel_foreground"
        const val CHANNEL_ID_ALERTS = "sentinel_alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1001
    }
}
