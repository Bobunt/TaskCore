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
import com.example.taskcore.data.TaskStatus
import com.example.taskcore.data.tables.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


data class AssigneeOption(
    val name: String
) {
    val label: String get() = name
}

data class TaskState(
    val mode: TaskMode = TaskMode.CREATE,

    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val assignee: String = "", // будем хранить login выбранного пользователя
    val dueDate: String = LocalDate.now().plusDays(1).toString(),
    val status: String = TaskStatus.OPEN.name,

    val assigneeOptions: List<AssigneeOption> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank()
}

class TaskViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private var usersLoaded = false
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    @RequiresApi(Build.VERSION_CODES.O)
    fun load(taskId: String?) {
        loadAssigneesIfNeeded()

        if (taskId == null) {
            _state.update { TaskState(mode = TaskMode.CREATE, assigneeOptions = it.assigneeOptions, assignee = it.assignee) }
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
    private fun parseDueDateToMillis(dueDate: String): Long {
        val dueLocalDate = LocalDate.parse(dueDate.trim()) // YYYY-MM-DD
        return dueLocalDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun parseStatus(status: String): com.example.taskcore.data.TaskStatus {
        return runCatching { com.example.taskcore.data.TaskStatus.valueOf(status.trim()) }
            .getOrElse { throw IllegalArgumentException("Неверный статус: $status") }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun errorMessage(e: Exception): String = when (e) {
        is java.time.format.DateTimeParseException ->
            "Неверный формат даты. Используй YYYY-MM-DD"
        is IllegalArgumentException ->
            e.message ?: "Некорректные данные"
        else ->
            "Ошибка операции"
    }

    fun onDeleteClick(onDeleted: () -> Unit) {
        val id = _state.value.taskId?.toIntOrNull()
        if (id == null || _state.value.isLoading) {
            _state.update { it.copy(error = "Нельзя удалить: нет id задачи") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val deletedRows = withContext(Dispatchers.IO) {
                    val existing = database.tasksDao().getById(id)
                    if (existing == null) 0 else database.tasksDao().delete(existing)
                }

                if (deletedRows == 0) {
                    _state.update { it.copy(isLoading = false, error = "Задача не найдена") }
                    return@launch
                }

                _state.update { it.copy(isLoading = false) }
                onDeleted()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка удаления задачи") }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onCreateClick() {
        val s = _state.value
        if (!s.canSave || s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val dueTimestamp = parseDueDateToMillis(s.dueDate)
                val status = parseStatus(s.status)

                val now = System.currentTimeMillis()
                val entity = Tasks(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    assignee = s.assignee.trim(),
                    dueDateTimestamp = dueTimestamp,
                    status = status,
                    createdAtTimestamp = now,
                    updatedAtTimestamp = now
                )

                val newId = withContext(Dispatchers.IO) {
                    database.tasksDao().insert(entity)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        mode = TaskMode.VIEW,
                        taskId = newId.toString()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = errorMessage(e)) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onSaveClick() {
        val s = _state.value
        if (!s.canSave || s.isLoading) return

        val id = s.taskId?.toIntOrNull()
        if (id == null) {
            _state.update { it.copy(error = "Нельзя сохранить: отсутствует id задачи") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val dueTimestamp = parseDueDateToMillis(s.dueDate)
                val status = parseStatus(s.status)
                val now = System.currentTimeMillis()

                // Важно: чтобы не потерять createdAtTimestamp — сначала читаем задачу из БД
                val existing = withContext(Dispatchers.IO) {
                    database.tasksDao().getById(id)
                }

                if (existing == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Задача не найдена (возможно, была удалена)"
                        )
                    }
                    return@launch
                }

                val updated = existing.copy(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    assignee = s.assignee.trim(),
                    dueDateTimestamp = dueTimestamp,
                    status = status,
                    updatedAtTimestamp = now
                )

                val rows = withContext(Dispatchers.IO) {
                    database.tasksDao().update(updated)
                }

                if (rows == 0) {
                    _state.update { it.copy(isLoading = false, error = "Не удалось сохранить изменения") }
                    return@launch
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        mode = TaskMode.VIEW
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = errorMessage(e).replace("операции", "сохранения")) }
            }
        }
    }

    private fun loadAssigneesIfNeeded() {
        if (usersLoaded) return
        usersLoaded = true

        viewModelScope.launch {
            try {
                val users = withContext(Dispatchers.IO) {
                    database.userDao.getAll()
                }

                val options = users.map { AssigneeOption(name = it.name) }

                _state.update { st ->
                    val defaultAssignee = st.assignee.ifBlank {
                        options.firstOrNull()?.name.orEmpty()
                    }
                    st.copy(
                        assigneeOptions = options,
                        assignee = defaultAssignee
                    )
                }
            } catch (e: Exception) {
                // не валим экран, просто покажем ошибку (или можно проигнорить)
                _state.update { it.copy(error = "Не удалось загрузить пользователей") }
            }
        }
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