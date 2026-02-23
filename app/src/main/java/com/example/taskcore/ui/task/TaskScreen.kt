package com.example.taskcore.ui.task

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskcore.ui.registration.RegistrationViewModel
import java.io.File
import java.time.LocalDate

enum class TaskMode { CREATE, VIEW, EDIT }

data class TaskFileUi(
    val id: Int,
    val fileName: String,
    val mimeType: String,
    val filePath: String
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    taskId: String?,
    onBack: () -> Unit,
    vm: TaskViewModel = viewModel(factory = TaskViewModel.factory)
) {

    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) vm.addFile(context, uri)
        }
    )

    LaunchedEffect(taskId) {
        vm.load(taskId)
    }

    val state by vm.state.collectAsState()

    val title = when (state.mode) {
        TaskMode.CREATE -> "Новая задача"
        TaskMode.VIEW -> "Задача"
        TaskMode.EDIT -> "Редактирование"
    }

    fun openAttachedFile(context: Context, filePath: String, mimeType: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Чтобы не падать, если нет подходящего приложения
        val chooser = Intent.createChooser(intent, "Открыть с помощью")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(chooser) }
            .onFailure {
                Toast.makeText(context, "Нет приложения для открытия этого файла", Toast.LENGTH_SHORT).show()
            }
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

            // ----- Вложения -----
            val canAttach = state.taskId != null && state.mode != TaskMode.CREATE && !readOnly
// если хочешь разрешить прикреплять и в VIEW, то:
// val canAttach = state.taskId != null && state.mode != TaskMode.CREATE

            Text("Вложения", style = MaterialTheme.typography.titleMedium)

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.taskId == null)
                                "Сначала создайте задачу"
                            else
                                "Файлов: ${state.files.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        IconButton(
                            onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                            enabled = canAttach && !state.isFilesLoading && !state.isLoading
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Прикрепить файл")
                        }
                    }

                    if (state.isFilesLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    when {
                        state.taskId == null -> {
                            Text("Вложения доступны после создания задачи.")
                        }
                        state.files.isEmpty() -> {
                            Text("Нет вложений.")
                        }
                        else -> {
                            // Скролл внутри карточки
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.files, key = { it.id }) { f ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // Открываем во внешнем приложении
                                                openAttachedFile(context, f.filePath, f.mimeType)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(f.fileName, style = MaterialTheme.typography.bodyLarge)
                                                Text(f.mimeType, style = MaterialTheme.typography.bodySmall)
                                            }

                                            IconButton(
                                                onClick = { vm.deleteFile(context, f.id) },
                                                enabled = !readOnly && !state.isFilesLoading && !state.isLoading
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Удалить файл")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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


