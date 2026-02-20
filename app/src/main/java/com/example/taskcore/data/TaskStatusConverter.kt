package com.example.taskcore.data

import androidx.room.TypeConverter

class TaskStatusConverter {

    @TypeConverter
    fun fromStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }
}