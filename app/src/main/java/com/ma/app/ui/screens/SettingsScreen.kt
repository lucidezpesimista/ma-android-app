package com.ma.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.ExportViewModel

/**
 * Pantalla de configuración y exportación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: NodeRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: ExportViewModel = viewModel(
        factory = ExportViewModel.provideFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launchers para Storage Access Framework
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { viewModel.exportToMarkdown(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromMarkdown(context, it) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { viewModel.backupToFile(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreFromFile(context, it) }
    }

    // Mostrar mensajes de éxito/error
    LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
        if (uiState.exportSuccess) {
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
            // Sección de Exportar
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

            // Sección de Importar
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

            // Sección de Backup
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

            // Sección de Información
            SettingsSection(title = "Acerca de") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "間 (ma)",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Outliner personal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Versión 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mensajes de estado
            if (uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            uiState.exportError?.let { error ->
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
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
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
