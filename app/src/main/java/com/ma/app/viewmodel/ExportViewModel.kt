package com.ma.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.repository.NodeRepository
import com.ma.app.utils.MarkdownExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Estado UI para exportación/backup.
 */
data class ExportUiState(
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportError: String? = null,
    val lastExportedContent: String? = null
)

/**
 * ViewModel para manejar exportación a Markdown e importación.
 */
class ExportViewModel(private val repository: NodeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val exporter = MarkdownExporter(repository)

    // ===== EXPORTACIÓN =====

    fun exportToMarkdown(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)

            try {
                val markdown = exporter.exportToMarkdown()

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(markdown)
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    lastExportedContent = markdown
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun exportBranchToMarkdown(context: Context, uri: Uri, nodeId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)

            try {
                val markdown = exporter.exportBranchToMarkdown(nodeId)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(markdown)
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    lastExportedContent = markdown
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    // ===== IMPORTACIÓN =====

    fun importFromMarkdown(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)

            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    } ?: throw IllegalStateException("No se pudo abrir el archivo")
                }

                val nodes = exporter.importFromMarkdown(content)

                // Insertar nodos (los IDs temporales serán reemplazados)
                nodes.forEach { node ->
                    repository.createChild(
                        parentId = node.parentId,
                        title = node.title
                    )
                    // Nota: La importación completa con notas requeriría
                    // un método más sofisticado en el repository
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    // ===== BACKUP / RESTORE =====

    fun backupToFile(context: Context, uri: Uri) {
        // Backup es igual a exportación por ahora
        exportToMarkdown(context, uri)
    }

    fun restoreFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)

            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    } ?: throw IllegalStateException("No se pudo abrir el archivo")
                }

                // Limpiar y reimportar
                val nodes = exporter.importFromMarkdown(content)

                // Aquí necesitaríamos un método para reemplazar todo el árbol
                // Por ahora, solo agregamos los nodos
                nodes.forEach { node ->
                    repository.createChild(node.parentId, node.title)
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = false,
            exportError = null
        )
    }

    companion object {
        fun provideFactory(repository: NodeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExportViewModel(repository) as T
                }
            }
        }
    }
}
