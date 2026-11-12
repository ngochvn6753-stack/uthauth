
package com.example.uthauth

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log

object TaskAlarmScheduler {

    private fun pendingIntent(
        context: Context,
        taskId: String,
        title: String
    ): PendingIntent {
        val intent = Intent(
            context,
            TaskReminderReceiver::class.java
        ).apply {
            action = "com.example.uthauth.TASK_REMINDER"
            data = android.net.Uri.parse(
                "uthauth://reminder/$taskId"
            )
            putExtra("taskTitle", title)
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(
        context: Context,
        taskId: String,
        title: String,
        date: String,
        time: String
    ) {
        val formatter = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val dateTime = try {
            formatter.parse("$date $time")
        } catch (e: Exception) {
            null
        } ?: return

        val triggerAt = dateTime.time
        if (triggerAt <= System.currentTimeMillis()) return

        val manager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val alarmIntent = pendingIntent(
            context,
            taskId,
            title
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !manager.canScheduleExactAlarms()
        ) {
            Log.w(
                "TaskAlarm",
                "Chưa có quyền báo thức chính xác"
            )

            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                alarmIntent
            )
        } else {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                alarmIntent
            )

            Log.d(
                "TaskAlarm",
                "Đã đặt báo thức chính xác: $date $time"
            )
        }
    }

    fun cancel(
        context: Context,
        taskId: String
    ) {
        val manager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        manager.cancel(
            pendingIntent(context, taskId, "")
        )
    }
}
