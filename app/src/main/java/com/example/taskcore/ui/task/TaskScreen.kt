package com.example.taskcore.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskcore.ui.registration.RegistrationViewModel
import java.time.LocalDate

enum class TaskMode { CREATE, VIEW, EDIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    taskId: String?,
    onBack: () -> Unit,
    vm: TaskViewModel = viewModel(factory = TaskViewModel.factory)
) {

    var showDeleteDialog by remember { mutableStateOf(false) }

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
                            TextButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !state.isLoading
                            ) { Text("Удалить") }
                        }

                        TaskMode.EDIT -> {
                            TextButton(
                                onClick = {
                                    vm.onSaveClick()
                                    onBack()
                                },
                                enabled = state.canSave && !state.isLoading
                            ) { Text("Сохранить") }

                            TextButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !state.isLoading
                            ) { Text("Удалить") }
                        }

                        TaskMode.CREATE -> {
                            TextButton(
                                onClick = {
                                    vm.onCreateClick()
                                    onBack()
                                },
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

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить задачу?") },
                text = { Text("Это действие нельзя отменить.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            vm.onDeleteClick(onDeleted = onBack)
                        },
                        enabled = !state.isLoading
                    ) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                }
            )
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

            AssigneeDropdown(
                assigneeLogin = state.assignee,
                options = state.assigneeOptions,
                readOnly = readOnly,
                onAssigneeSelected = vm::onAssigneeChanged
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

            StatusDropdown(
                currentStatus = state.status,
                readOnly = readOnly,
                onStatusSelected = vm::onStatusChanged
            )

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssigneeDropdown(
    assigneeLogin: String,
    options: List<AssigneeOption>,
    readOnly: Boolean,
    onAssigneeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = options.firstOrNull { it.name == assigneeLogin }?.label
        ?: assigneeLogin.ifBlank { "—" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!readOnly) expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ответственный") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.label) },
                    onClick = {
                        onAssigneeSelected(user.name) // сохраняем login
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    currentStatus: String,
    readOnly: Boolean,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statuses = com.example.taskcore.data.TaskStatus.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (!readOnly) expanded = !expanded
        }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = currentStatus,
            onValueChange = {},
            readOnly = true,
            label = { Text("Статус") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onStatusSelected(status.name)
                        expanded = false
                    }
                )
            }
        }
    }
}
