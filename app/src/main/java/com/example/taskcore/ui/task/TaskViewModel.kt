package com.example.taskcore.ui.task

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskcore.App
import com.example.taskcore.data.TaskCoreDB
import com.example.taskcore.data.TaskStatus
import com.example.taskcore.data.tables.TaskFiles
import com.example.taskcore.data.tables.Tasks
import com.example.taskcore.ui.common.dbViewModelFactory
import com.example.taskcore.ui.taskslist.TaskListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


data class AssigneeOption(
    val login: String
) {
    val label: String get() = login
}

data class TaskState(
    val mode: TaskMode = TaskMode.CREATE,

    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val assignee: String = "",
    val dueDate: String = LocalDate.now().plusDays(1).toString(),
    val status: String = TaskStatus.OPEN.name,

    val assigneeOptions: List<AssigneeOption> = emptyList(),

    val files: List<TaskFileUi> = emptyList(),
    val isFilesLoading: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank()
}

@RequiresApi(Build.VERSION_CODES.O)
class TaskViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private var usersLoaded = false
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    fun load(taskId: String?) {
        loadAssigneesIfNeeded()

        if (taskId == null) {
            _state.update { it.copy(mode = TaskMode.CREATE, taskId = null, files = emptyList(), error = null) }
            return
        }

        val id = taskId.toIntOrNull()
        if (id == null) {
            _state.update { it.copy(error = "Некорректный id задачи", files = emptyList()) }
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
                            mode = TaskMode.VIEW,
                            taskId = taskId,
                            error = "Задача не найдена",
                            files = emptyList()
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

                loadFiles(entity.id)

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки задачи",
                        files = emptyList()
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

    private fun loadFiles(taskId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isFilesLoading = true) }
            try {
                val entities = withContext(Dispatchers.IO) {
                    database.taskFilesDao().getByTaskId(taskId)
                }
                val ui = entities.map {
                    TaskFileUi(
                        id = it.id,
                        fileName = it.fileName,
                        mimeType = it.mimeType,
                        filePath = it.filePath
                    )
                }
                _state.update { it.copy(files = ui, isFilesLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isFilesLoading = false, error = "Ошибка загрузки файлов") }
            }
        }
    }

    fun addFile(context: Context, uri: Uri) {
        val taskId = _state.value.taskId?.toIntOrNull()
        if (taskId == null) {
            _state.update { it.copy(error = "Сначала создайте задачу, потом прикрепляйте файлы") }
            return
        }
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isFilesLoading = true, error = null) }

            try {
                val createdId = withContext(Dispatchers.IO) {
                    val cr = context.contentResolver
                    val mime = cr.getType(uri) ?: "application/octet-stream"
                    val name = queryDisplayName(cr, uri) ?: "file_${System.currentTimeMillis()}"

                    // Копируем файл в приватное хранилище приложения
                    val dir = File(context.filesDir, "task_files/$taskId").apply { mkdirs() }
                    val target = File(dir, "${System.currentTimeMillis()}_$name")

                    cr.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Не удалось открыть файл" }
                        target.outputStream().use { output -> input.copyTo(output) }
                    }

                    val now = System.currentTimeMillis()
                    val entity = TaskFiles(
                        taskId = taskId,
                        fileName = name,
                        filePath = target.absolutePath,
                        mimeType = mime,
                        createdAtTimestamp = now
                    )
                    database.taskFilesDao().insert(entity).toInt()
                }

                // Перезагружаем список
                loadFiles(taskId)
                _state.update { it.copy(isFilesLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isFilesLoading = false, error = "Ошибка добавления файла") }
            }
        }
    }

    private fun queryDisplayName(cr: android.content.ContentResolver, uri: Uri): String? {
        val cursor = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            return if (index >= 0) it.getString(index) else null
        }
    }

    fun deleteFile(context: Context, fileId: Int) {
        val taskId = _state.value.taskId?.toIntOrNull() ?: return

        viewModelScope.launch {
            _state.update { it.copy(isFilesLoading = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val entity = database.taskFilesDao().getById(fileId)
                    if (entity != null) {
                        // удаляем физический файл
                        runCatching { File(entity.filePath).delete() }
                        database.taskFilesDao().delete(entity)
                    }
                }
                loadFiles(taskId)
                _state.update { it.copy(isFilesLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isFilesLoading = false, error = "Ошибка удаления файла") }
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

                val options = users.map { AssigneeOption(login = it.login) }

                _state.update { st ->
                    val defaultAssignee = st.assignee.ifBlank {
                        options.firstOrNull()?.login.orEmpty()
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
        val factory = dbViewModelFactory { TaskViewModel(it) }
    }
}