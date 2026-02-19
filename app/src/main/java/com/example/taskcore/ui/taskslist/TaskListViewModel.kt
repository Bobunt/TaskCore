package com.example.taskcore.ui.taskslist

import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

data class TaskListState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null
)

class TaskListViewModel : ViewModel() {

    private val _state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MutableStateFlow(
            TaskListState(
                tasks = listOf(
                    Task(
                        id = "1",
                        title = "Подготовить отчет",
                        description = "Собрать метрики за неделю",
                        dueDate = LocalDate.now().plusDays(1),
                        status = TaskStatus.OPEN,
                        assignee = "Текущий пользователь"
                    ),
                    Task(
                        id = "2",
                        title = "Созвон с командой",
                        description = "Обсудить релиз и риски",
                        dueDate = LocalDate.now().plusDays(2),
                        status = TaskStatus.IN_PROGRESS,
                        assignee = "Текущий пользователь"
                    ),
                    Task(
                        id = "3",
                        title = "Проверить баги",
                        description = "Регрессия после обновления",
                        dueDate = LocalDate.now().plusDays(3),
                        status = TaskStatus.DONE,
                        assignee = "Текущий пользователь"
                    )
                )
            )
        )
    } else {
        TODO("VERSION.SDK_INT < O")
    }

    val state: StateFlow<TaskListState> = _state
}