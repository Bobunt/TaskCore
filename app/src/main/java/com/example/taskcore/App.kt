package com.example.taskcore

import android.app.Application
import com.example.taskcore.data.TaskCoreDB
import com.example.taskcore.data.workers.OverdueTasksScheduler

class App: Application() {
    val database by lazy { TaskCoreDB.createDataBase(this) }

    override fun onCreate() {
        super.onCreate()
        OverdueTasksScheduler.schedule(this)
    }
}