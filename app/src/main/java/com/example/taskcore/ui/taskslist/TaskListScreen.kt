package com.example.taskcore.ui.taskslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (taskId: String) -> Unit,
    onCreateTaskClick: () -> Unit,
    vm: TaskListViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

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
                            onClick = { onTaskClick(task.id) }
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
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
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