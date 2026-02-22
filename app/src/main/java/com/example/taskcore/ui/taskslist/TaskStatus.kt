package com.example.taskcore.ui.taskslist

import com.example.taskcore.data.TaskStatus
import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val status: TaskStatus,
    val assignee: String
)