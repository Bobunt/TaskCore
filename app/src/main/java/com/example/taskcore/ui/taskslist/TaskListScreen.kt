package com.example.taskcore.ui.taskslist

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskcore.ui.task.TaskViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (taskId: String) -> Unit,
    onCreateTaskClick: () -> Unit,
    vm: TaskListViewModel = viewModel(factory = TaskListViewModel.factory)
) {
    val state by vm.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Список задач") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTaskClick) {
                Text("+")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onDeleteClick = { vm.deleteTask(it.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onDeleteClick: (Task) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить задачу?") },
            text = { Text("Задача \"${task.title}\" будет удалена.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(task)
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить"
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                task.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(task.status.name) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("до ${task.dueDate}") }
                )
            }
        }
    }
}