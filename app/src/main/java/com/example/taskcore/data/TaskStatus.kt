package com.example.taskcore.data

enum class TaskStatus {
    OPEN,         // Задача создана, но не начата
    IN_PROGRESS,  // В процессе выполнения
    DONE,         // Успешно завершена
    FAILED        // Завершена неуспешно
}