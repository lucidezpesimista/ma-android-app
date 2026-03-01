package com.ma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.Node
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado UI para la pantalla de detalle de nodo (nota).
 */
data class NodeDetailUiState(
    val node: Node? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)

/**
 * ViewModel para editar la nota/detalle de un nodo.
 */
class NodeDetailViewModel(
    private val repository: NodeRepository,
    private val nodeId: Long
) : ViewModel() {

    private val _noteDraft = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NodeDetailUiState> = combine(
        repository.getNodeById(nodeId),
        _noteDraft,
        _isLoading,
        _error
    ) { node, draft, loading, error ->
        val hasChanges = draft != null && draft != node?.note
        NodeDetailUiState(
            node = node?.copy(note = draft ?: node.note),
            isLoading = loading,
            error = error,
            hasUnsavedChanges = hasChanges
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NodeDetailUiState(isLoading = true)
    )

    // ===== EDICIÓN =====

    fun updateNote(newNote: String) {
        _noteDraft.value = newNote
    }

    fun saveNote() {
        viewModelScope.launch {
            val draft = _noteDraft.value ?: return@launch
            repository.updateNote(nodeId, draft.takeIf { it.isNotBlank() })
            _noteDraft.value = null
        }
    }

    fun discardChanges() {
        _noteDraft.value = null
    }

    // ===== TÍTULO =====

    fun updateTitle(newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(nodeId, newTitle)
        }
    }

    companion object {
        fun provideFactory(repository: NodeRepository, nodeId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NodeDetailViewModel(repository, nodeId) as T
                }
            }
        }
    }
}
