package com.example.taskcore.data.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_files",
    foreignKeys = [
        ForeignKey(
            entity = Tasks::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["task_id"])
    ]
)
data class TaskFiles(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Int,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "created_at_timestamp")
    val createdAtTimestamp: Long
)