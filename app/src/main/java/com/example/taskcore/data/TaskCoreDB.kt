package com.example.taskcore.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taskcore.data.dao.TaskFilesDao
import com.example.taskcore.data.dao.TasksDao
import com.example.taskcore.data.dao.UsersDao
import com.example.taskcore.data.tables.TaskFiles
import com.example.taskcore.data.tables.Tasks
import com.example.taskcore.data.tables.Users

@Database(
    entities = [Users::class, Tasks::class, TaskFiles::class],
    version = 4
)
@TypeConverters(TaskStatusConverter::class)
abstract class TaskCoreDB: RoomDatabase() {
    abstract val userDao: UsersDao
    abstract fun tasksDao(): TasksDao
    abstract fun taskFilesDao(): TaskFilesDao
    companion object {
        fun createDataBase(context: Context): TaskCoreDB {
            return Room.databaseBuilder(
                context,
                TaskCoreDB::class.java,
                "taskcore.db"
            ).build()
        }
    }
}