package com.example.taskcore

import android.app.Application
import com.example.taskcore.data.TaskCoreDB

class App: Application() {
    val database by lazy { TaskCoreDB.createDataBase(this) }
}