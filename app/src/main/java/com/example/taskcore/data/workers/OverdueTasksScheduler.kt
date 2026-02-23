package com.example.taskcore.data.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object OverdueTasksScheduler {

    private const val UNIQUE_WORK_NAME = "overdue_tasks_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<OverdueTasksWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

}