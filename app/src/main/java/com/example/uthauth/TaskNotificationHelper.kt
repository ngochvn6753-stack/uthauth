
package com.example.uthauth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object TaskNotificationHelper {

    private const val CHANNEL_ID = "task_reminders"

    fun showNotification(
        context: Context,
        taskTitle: String
    ) {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở công việc",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi đến giờ làm việc"
            }

            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Đến giờ làm việc!")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (
            Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        }
    }
}
