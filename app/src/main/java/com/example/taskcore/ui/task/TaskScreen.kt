package com.example.taskcore.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

enum class TaskMode { CREATE, VIEW, EDIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    taskId: String?,
    onBack: () -> Unit,
    vm: TaskViewModel = viewModel()
) {
    LaunchedEffect(taskId) {
        vm.load(taskId)
    }

    val state by vm.state.collectAsState()

    val title = when (state.mode) {
        TaskMode.CREATE -> "Новая задача"
        TaskMode.VIEW -> "Задача"
        TaskMode.EDIT -> "Редактирование"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
                actions = {
                    when (state.mode) {
                        TaskMode.VIEW -> {
                            TextButton(onClick = vm::toEdit) { Text("Изменить") }
                        }
                        TaskMode.EDIT -> {
                            TextButton(
                                onClick = vm::onSaveClick,
                                enabled = state.canSave && !state.isLoading
                            ) { Text("Сохранить") }
                        }
                        TaskMode.CREATE -> {
                            TextButton(
                                onClick = vm::onCreateClick,
                                enabled = state.canSave && !state.isLoading
                            ) { Text("Создать") }
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val readOnly = state.mode == TaskMode.VIEW

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.title,
                onValueChange = vm::onTitleChanged,
                label = { Text("Наименование задачи") },
                singleLine = true,
                readOnly = readOnly
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                value = state.description,
                onValueChange = vm::onDescriptionChanged,
                label = { Text("Описание") },
                readOnly = readOnly
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.assignee,
                onValueChange = vm::onAssigneeChanged,
                label = { Text("Ответственный") },
                singleLine = true,
                readOnly = readOnly
            )

            // Пока дата и статус как текстовые поля (потом заменим на DatePicker/Dropdown)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.dueDate,
                onValueChange = vm::onDueDateChanged,
                label = { Text("Дата завершения (YYYY-MM-DD)") },
                singleLine = true,
                readOnly = readOnly
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.status,
                onValueChange = vm::onStatusChanged,
                label = { Text("Статус (OPEN/IN_PROGRESS/DONE)") },
                singleLine = true,
                readOnly = readOnly
            )

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
