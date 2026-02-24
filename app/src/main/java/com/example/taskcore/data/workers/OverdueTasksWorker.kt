package com.example.taskcore.data.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskcore.App

class OverdueTasksWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val db = app.database
        val dao = db.tasksDao()

        val now = System.currentTimeMillis()

        val overdue = dao.getOverdueNotNotified(now = now, doneStatus = "DONE")
        if (overdue.isEmpty()) return Result.success()

        // Формируем список названий
        val titles = overdue.joinToString(separator = "\n") { "• ${it.title}" }

        showNotification(
            context = applicationContext,
            title = "Просроченные задачи: ${overdue.size}",
            text = titles
        )

        dao.markOverdueNotified(overdue.map { it.id }, ts = now)

        return Result.success()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Просроченные задачи",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(context: Context, title: String, text: String) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Разверните для просмотра")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "overdue_tasks"
        private const val NOTIF_ID = 1001
    }
}