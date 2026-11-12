
package com.example.uthauth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val taskTitle = intent.getStringExtra("taskTitle")
            ?: "Bạn có công việc cần hoàn thành"

        TaskNotificationHelper.showNotification(
            context = context,
            taskTitle = taskTitle
        )
    }
}
