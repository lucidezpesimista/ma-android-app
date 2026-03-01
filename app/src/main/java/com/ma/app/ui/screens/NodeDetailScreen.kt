package com.ma.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
nimport androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.NodeDetailViewModel

/**
 * Pantalla de detalle de nodo para editar la nota.
 * 
 * DECISIÓN: Mantenemos el título editable arriba y la nota abajo.
 * Esto permite editar ambos sin necesidad de una pantalla separada
 * solo para el título.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    nodeId: Long,
    repository: NodeRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: NodeDetailViewModel = viewModel(
        factory = NodeDetailViewModel.provideFactory(repository, nodeId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        uiState.node?.title?.takeIf { it.isNotBlank() } ?: "Nota",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) {
                            viewModel.saveNote()
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (uiState.hasUnsavedChanges) {
                        TextButton(onClick = { viewModel.saveNote() }) {
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
            // Campo de título
            uiState.node?.let { node ->
                OutlinedTextField(
                    value = node.title,
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

                // Campo de nota
                Text(
                    text = "Nota",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = node.note ?: "",
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
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (node.note.isNullOrBlank()) {
                                Text(
                                    text = "Escribe tu nota aquí...\n\n" +
                                           "Puedes usar #hashtags y [[links internos]]",
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
}
