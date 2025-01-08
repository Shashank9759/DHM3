package com.example.dhm20.Data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import androidx.core.app.NotificationCompat
import com.example.dhm20.R

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule your notification or any task here
            scheduleNotification(context)
        }
    }

    private fun scheduleNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reboot_channel",
                "Reboot Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, "reboot_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Run DHM App")

            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Send the notification
        notificationManager.notify(1, notification)
    }
}