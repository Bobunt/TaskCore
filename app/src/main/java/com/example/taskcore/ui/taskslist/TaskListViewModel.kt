package com.example.taskcore.ui.taskslist

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TaskListState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class TaskListViewModel(
    private val database: TaskCoreDB
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListState(isLoading = true))
    val state: StateFlow<TaskListState> = _state

    init {
        loadTasks()
    }

    fun refresh() {
        loadTasks()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val entities: List<Tasks> = withContext(Dispatchers.IO) {
                    database.tasksDao().getAll()
                }

                // Entity -> UI model
                val tasks: List<Task> = entities.map { entity ->
                    Task(
                        id = entity.id.toString(),
                        title = entity.title,
                        description = entity.description,
                        assignee = entity.assignee,
                        dueDate = epochMillisToLocalDate(entity.dueDateTimestamp),
                        status = entity.status // см. примечание ниже
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        tasks = tasks,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки задач"
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
        // Для dueDate чаще логично использовать локальную таймзону пользователя
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun deleteTask(taskId: String) {
        val id = taskId.toIntOrNull() ?: return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val entity = database.tasksDao().getById(id)
                    if (entity != null) {
                        database.tasksDao().delete(entity)
                    }
                }

                // Обновляем список
                refresh()

            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка удаления задачи") }
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val database = (checkNotNull(extras[APPLICATION_KEY]) as App).database
                return TaskListViewModel(database) as T
            }
        }
    }
}