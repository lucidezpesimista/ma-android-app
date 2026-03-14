package com.ma.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.model.Node
import com.ma.app.data.model.Priority
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    repository: NodeRepository,
    onNavigateToNode: (Long) -> Unit = {}
) {
    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.provideFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas") },
                actions = {
                    IconButton(onClick = viewModel::toggleShowCompleted) {
                        Icon(
                            if (uiState.showCompleted) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = "Ver completadas"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva tarea")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Tareas vencidas
            if (uiState.overdueTasks.isNotEmpty()) {
                item {
                    TaskSectionHeader(
                        title = "Vencidas",
                        count = uiState.overdueTasks.size,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(uiState.overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    TaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleCompleted(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onSetPriority = { viewModel.setPriority(task.id, it) },
                        onClick = { onNavigateToNode(task.id) },
                        isOverdue = true
                    )
                }
            }

            // Tareas pendientes
            if (uiState.pendingTasks.isNotEmpty()) {
                item {
                    TaskSectionHeader(
                        title = "Pendientes",
                        count = uiState.pendingTasks.size
                    )
                }
                items(uiState.pendingTasks, key = { "pending_${it.id}" }) { task ->
                    TaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleCompleted(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onSetPriority = { viewModel.setPriority(task.id, it) },
                        onClick = { onNavigateToNode(task.id) }
                    )
                }
            }

            // Tareas completadas (colapsable)
            if (uiState.showCompleted && uiState.completedTasks.isNotEmpty()) {
                item {
                    TaskSectionHeader(
                        title = "Completadas",
                        count = uiState.completedTasks.size,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.completedTasks, key = { "done_${it.id}" }) { task ->
                    TaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleCompleted(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onSetPriority = { viewModel.setPriority(task.id, it) },
                        onClick = { onNavigateToNode(task.id) }
                    )
                }
            }

            // Estado vacío
            if (uiState.pendingTasks.isEmpty() && uiState.overdueTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "¡Todo al día!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Toca + para agregar una tarea",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para nueva tarea
    if (showAddDialog) {
        AddTaskDialog(
            onConfirm = { title ->
                viewModel.createTask(title)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun TaskSectionHeader(
    title: String,
    count: Int,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItem(
    task: Node,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onSetPriority: (Priority) -> Unit,
    onClick: () -> Unit,
    isOverdue: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val priority = task.priorityEnum()
    val priorityColor = when (priority) {
        Priority.HIGH -> Color(0xFFD32F2F)
        Priority.MEDIUM -> Color(0xFFFF8F00)
        Priority.LOW -> Color(0xFF388E3C)
        Priority.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val dateFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))

    ListItem(
        headlineContent = {
            Text(
                text = task.title.ifBlank { "(sin título)" },
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.dueDate?.let { date ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isOverdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!task.note.isNullOrBlank()) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            // Checkbox circular con color de prioridad
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (task.isCompleted) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Completar",
                    tint = if (task.isCompleted) Color(0xFF388E3C) else priorityColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver detalle") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = { showMenu = false; onClick() }
                    )
                    Divider()
                    Text(
                        "Prioridad",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Priority.entries.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (p) {
                                        Priority.HIGH -> "Alta"
                                        Priority.MEDIUM -> "Media"
                                        Priority.LOW -> "Baja"
                                        Priority.NONE -> "Sin prioridad"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Flag,
                                    null,
                                    tint = when (p) {
                                        Priority.HIGH -> Color(0xFFD32F2F)
                                        Priority.MEDIUM -> Color(0xFFFF8F00)
                                        Priority.LOW -> Color(0xFF388E3C)
                                        Priority.NONE -> MaterialTheme.colorScheme.outline
                                    }
                                )
                            },
                            onClick = { onSetPriority(p); showMenu = false }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )

    Divider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun AddTaskDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nombre de la tarea") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
