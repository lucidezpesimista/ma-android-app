package com.ma.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.model.NodeType
import com.ma.app.data.model.Priority
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.NodeDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    nodeId: Long,
    repository: NodeRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: NodeDetailViewModel = viewModel(
        factory = NodeDetailViewModel.provideFactory(repository, nodeId, context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showClaudeMenu by remember { mutableStateOf(false) }

    val node = uiState.node

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        node?.title?.takeIf { it.isNotBlank() } ?: "Nota",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) viewModel.saveNote()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón Claude AI
                    Box {
                        IconButton(onClick = { showClaudeMenu = true }) {
                            if (uiState.isClaudeLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.SmartToy, contentDescription = "Claude AI")
                            }
                        }
                        DropdownMenu(
                            expanded = showClaudeMenu,
                            onDismissRequest = { showClaudeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Corregir ortografía") },
                                leadingIcon = { Icon(Icons.Default.Spellcheck, null) },
                                onClick = { viewModel.spellCheckNote(); showClaudeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Resumir nota") },
                                leadingIcon = { Icon(Icons.Default.Summarize, null) },
                                onClick = { viewModel.summarizeNote(); showClaudeMenu = false }
                            )
                        }
                    }

                    // Compartir
                    IconButton(onClick = {
                        val text = buildString {
                            append(node?.title ?: "")
                            if (!node?.note.isNullOrBlank()) {
                                append("\n\n")
                                append(node?.note)
                            }
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, node?.title ?: "Nota")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir nota"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }

                    // Guardar
                    if (uiState.hasUnsavedChanges) {
                        TextButton(onClick = viewModel::saveNote) {
                            Text("Guardar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            node?.let { n ->

                // Chips de tipo de nodo y prioridad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tipo de nodo
                    FilterChip(
                        selected = n.nodeTypeEnum() == NodeType.TASK,
                        onClick = {
                            if (n.nodeTypeEnum() == NodeType.TASK) viewModel.convertToNote()
                            else viewModel.convertToTask()
                        },
                        label = {
                            Text(
                                if (n.nodeTypeEnum() == NodeType.TASK) "Tarea" else "Nota"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (n.nodeTypeEnum() == NodeType.TASK) Icons.Default.CheckBox
                                else Icons.Default.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Prioridad (solo si es tarea)
                    if (n.nodeTypeEnum() == NodeType.TASK) {
                        Box {
                            FilterChip(
                                selected = n.priorityEnum() != Priority.NONE,
                                onClick = { showPriorityMenu = true },
                                label = {
                                    Text(
                                        when (n.priorityEnum()) {
                                            Priority.HIGH -> "Alta"
                                            Priority.MEDIUM -> "Media"
                                            Priority.LOW -> "Baja"
                                            Priority.NONE -> "Prioridad"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = when (n.priorityEnum()) {
                                            Priority.HIGH -> Color(0xFFD32F2F)
                                            Priority.MEDIUM -> Color(0xFFFF8F00)
                                            Priority.LOW -> Color(0xFF388E3C)
                                            Priority.NONE -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showPriorityMenu,
                                onDismissRequest = { showPriorityMenu = false }
                            ) {
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
                                        onClick = {
                                            viewModel.setPriority(p)
                                            showPriorityMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Fecha límite
                        FilterChip(
                            selected = n.dueDate != null,
                            onClick = { showDatePicker = true },
                            label = {
                                Text(
                                    n.dueDate?.let {
                                        SimpleDateFormat("d MMM", Locale("es", "ES")).format(it)
                                    } ?: "Fecha"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Si es tarea: checkbox de completado
                if (n.nodeTypeEnum() == NodeType.TASK) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = n.isCompleted,
                            onCheckedChange = { viewModel.toggleCompleted() }
                        )
                        Text(
                            text = if (n.isCompleted) "Completada" else "Marcar como completada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (n.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de título
                OutlinedTextField(
                    value = n.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Título") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Resultado de Claude
                uiState.claudeResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Claude",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::clearClaudeResult,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Etiqueta de nota
                Text(
                    text = "Nota",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo de nota (markdown libre)
                BasicTextField(
                    value = n.note ?: "",
                    onValueChange = viewModel::updateNote,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (n.note.isNullOrBlank()) {
                                Text(
                                    text = "Escribe tu nota aquí...\n\n" +
                                            "Usa **negrita**, *cursiva*, `código`\n" +
                                            "Agrega #hashtags y [[links internos]]",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    // DatePicker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = node?.dueDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDueDate(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                Row {
                    if (node?.dueDate != null) {
                        TextButton(onClick = {
                            viewModel.setDueDate(null)
                            showDatePicker = false
                        }) {
                            Text("Quitar fecha")
                        }
                    }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
