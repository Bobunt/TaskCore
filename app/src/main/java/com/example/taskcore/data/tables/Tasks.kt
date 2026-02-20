package com.example.taskcore.data.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.taskcore.data.TaskStatus

@Entity(tableName = "tasks")
data class Tasks(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val description: String,
    val assignee: String,

    @ColumnInfo(name = "due_date_timestamp")
    val dueDateTimestamp: Long,

    val status: TaskStatus = TaskStatus.OPEN,

    @ColumnInfo(name = "created_at_timestamp")
    val createdAtTimestamp: Long,

    @ColumnInfo(name = "updated_at_timestamp")
    val updatedAtTimestamp: Long
)