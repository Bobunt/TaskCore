package com.example.taskcore.ui.task

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskcore.App
import com.example.taskcore.data.TaskCoreDB
import com.example.taskcore.data.tables.Tasks
import com.example.taskcore.ui.taskslist.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

class TaskViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    @RequiresApi(Build.VERSION_CODES.O)
    fun load(taskId: String?) {
        if (taskId == null) {
            // Create mode defaults
            _state.update { TaskState(mode = TaskMode.CREATE) }
            return
        }

        val id = taskId.toIntOrNull()
        if (id == null) {
            _state.update { it.copy(isLoading = false, error = "Некорректный id задачи") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val entity = withContext(Dispatchers.IO) {
                    database.tasksDao().getById(id)
                }

                if (entity == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Задача не найдена",
                            mode = TaskMode.VIEW,
                            taskId = taskId
                        )
                    }
                    return@launch
                }

                val dueDateStr = Instant.ofEpochMilli(entity.dueDateTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()

                _state.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        mode = TaskMode.VIEW,
                        taskId = entity.id.toString(),
                        title = entity.title,
                        description = entity.description,
                        assignee = entity.assignee,
                        dueDate = dueDateStr,
                        status = entity.status.name
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки задачи"
                    )
                }
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun onCreateClick() {
        val s = _state.value
        if (!s.canSave || s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // 1) Парсим дату
                val dueLocalDate = LocalDate.parse(s.dueDate)

                // 2) Дата -> timestamp (миллисекунды)
                val dueTimestamp = dueLocalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // 3) Парсим статус (и ловим ошибку)
                val status = runCatching { TaskStatus.valueOf(s.status.trim()) }
                    .getOrElse { throw IllegalArgumentException("Неверный статус: ${s.status}") }

                // 4) Создаём entity
                val now = System.currentTimeMillis()
                val entity = Tasks(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    assignee = s.assignee.trim(),
                    dueDateTimestamp = dueTimestamp,
                    status = com.example.taskcore.data.TaskStatus.valueOf(s.status.trim()), // см. примечание ниже про типы
                    createdAtTimestamp = now,
                    updatedAtTimestamp = now
                )

                // 5) Пишем в БД (IO)
                val newId = withContext(Dispatchers.IO) {
                    database.tasksDao().insert(entity) // insert вернёт Long id
                }

                // 6) Обновляем state и переходим в VIEW
                _state.update {
                    it.copy(
                        isLoading = false,
                        mode = TaskMode.VIEW,
                        taskId = newId.toString()
                    )
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is java.time.format.DateTimeParseException ->
                        "Неверный формат даты. Используй YYYY-MM-DD"
                    is IllegalArgumentException ->
                        e.message ?: "Некорректные данные"
                    else ->
                        "Ошибка создания задачи"
                }

                _state.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    fun onSaveClick() {
        val s = _state.value
        if (!s.canSave) return

        // Заглушка: "сохранили"
        _state.update { it.copy(mode = TaskMode.VIEW) }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val database = (checkNotNull(extras[APPLICATION_KEY]) as App).database
                return TaskViewModel(database) as T
            }
        }
    }
}