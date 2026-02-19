package com.example.taskcore.ui.taskslist

import java.time.LocalDate

enum class TaskStatus { OPEN, IN_PROGRESS, DONE }

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val status: TaskStatus,
    val assignee: String
)