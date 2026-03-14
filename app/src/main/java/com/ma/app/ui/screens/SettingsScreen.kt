package com.ma.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.ClaudeViewModel
import com.ma.app.viewmodel.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: NodeRepository,
    onNavigateBack: () -> Unit
) {
    val exportViewModel: ExportViewModel = viewModel(
        factory = ExportViewModel.provideFactory(repository)
    )
    val context = LocalContext.current
    val claudeViewModel: ClaudeViewModel = viewModel(
        factory = ClaudeViewModel.provideFactory(context)
    )
    val exportState by exportViewModel.uiState.collectAsStateWithLifecycle()
    val claudeState by claudeViewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let { exportViewModel.exportToMarkdown(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { exportViewModel.importFromMarkdown(context, it) } }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let { exportViewModel.backupToFile(context, it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { exportViewModel.restoreFromFile(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // ===== SECCIÓN CLAUDE AI =====
            SettingsSection(title = "Claude AI") {
                ClaudeApiKeySection(
                    currentKey = claudeState.apiKey,
                    hasKey = claudeState.hasApiKey,
                    onSaveKey = claudeViewModel::saveApiKey
                )
            }

            // ===== SECCIÓN EXPORTAR =====
            SettingsSection(title = "Exportar") {
                SettingsItem(
                    icon = Icons.Default.FileDownload,
                    title = "Exportar a Markdown",
                    subtitle = "Guarda todo tu árbol como archivo .md",
                    onClick = {
                        exportLauncher.launch("ma_export_${System.currentTimeMillis()}.md")
                    }
                )
            }

            // ===== SECCIÓN IMPORTAR =====
            SettingsSection(title = "Importar") {
                SettingsItem(
                    icon = Icons.Default.FileUpload,
                    title = "Importar desde Markdown",
                    subtitle = "Carga un archivo .md con estructura de árbol",
                    onClick = {
                        importLauncher.launch(arrayOf("text/markdown", "text/plain"))
                    }
                )
            }

            // ===== SECCIÓN BACKUP =====
            SettingsSection(title = "Backup") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Crear backup",
                    subtitle = "Guarda una copia de seguridad completa",
                    onClick = {
                        backupLauncher.launch("ma_backup_${System.currentTimeMillis()}.md")
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restaurar backup",
                    subtitle = "Reemplaza todo el contenido con un backup",
                    onClick = {
                        restoreLauncher.launch(arrayOf("text/markdown", "text/plain"))
                    }
                )
            }

            // ===== SECCIÓN ACERCA DE =====
            SettingsSection(title = "Acerca de") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("間 (ma)", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Outliner personal con tareas, calendario y Claude AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Versión 2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progreso de exportación
            if (exportState.isExporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            exportState.exportError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaudeApiKeySection(
    currentKey: String,
    hasKey: Boolean,
    onSaveKey: (String) -> Unit
) {
    var editingKey by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "API Key de Anthropic",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (hasKey) "Configurada ✓" else "No configurada",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasKey) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { isEditing = !isEditing }) {
                Text(if (isEditing) "Cancelar" else "Editar")
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editingKey,
                onValueChange = { editingKey = it },
                label = { Text("sk-ant-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ver clave"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Obtén tu API key en console.anthropic.com",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onSaveKey(editingKey.trim())
                    isEditing = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = editingKey.isNotBlank()
            ) {
                Text("Guardar API Key")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
