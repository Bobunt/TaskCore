package com.example.taskcore.ui.task

import android.os.Build
import androidx.lifecycle.ViewModel
import com.example.taskcore.ui.taskslist.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class TaskState(
    val mode: TaskMode = TaskMode.CREATE,

    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val assignee: String = "Текущий пользователь",
    val dueDate: String = LocalDate.now().plusDays(1).toString(),
    val status: String = TaskStatus.OPEN.name,

    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank()
}

class TaskViewModel : ViewModel() {

    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    fun load(taskId: String?) {
        if (taskId == null) {
            // Create mode defaults
            _state.update { TaskState(mode = TaskMode.CREATE) }
            return
        }

        // View mode + моковая загрузка "из базы"
        _state.update { it.copy(isLoading = true, error = null) }

        // Заглушка: просто подставим тестовую задачу
        _state.update {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.copy(
                    isLoading = false,
                    mode = TaskMode.VIEW,
                    taskId = taskId,
                    title = "Задача #$taskId",
                    description = "Описание задачи #$taskId",
                    assignee = "Текущий пользователь",
                    dueDate = LocalDate.now().plusDays(1).toString(),
                    status = TaskStatus.OPEN.name
                )
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        }
    }

    fun toEdit() {
        _state.update { it.copy(mode = TaskMode.EDIT, error = null) }
    }

    fun onTitleChanged(v: String) = _state.update { it.copy(title = v, error = null) }
    fun onDescriptionChanged(v: String) = _state.update { it.copy(description = v, error = null) }
    fun onAssigneeChanged(v: String) = _state.update { it.copy(assignee = v, error = null) }
    fun onDueDateChanged(v: String) = _state.update { it.copy(dueDate = v, error = null) }
    fun onStatusChanged(v: String) = _state.update { it.copy(status = v, error = null) }

    fun onCreateClick() {
        val s = _state.value
        if (!s.canSave) return

        // Заглушка: "создали"
        _state.update { it.copy(mode = TaskMode.VIEW, taskId = "new_id") }
    }

    fun onSaveClick() {
        val s = _state.value
        if (!s.canSave) return

        // Заглушка: "сохранили"
        _state.update { it.copy(mode = TaskMode.VIEW) }
    }
}