package com.ma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.Node
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
nimport kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado UI para la pantalla de outline.
 */
data class OutlineUiState(
    val currentNodeId: Long? = null, // null = root
    val nodes: List<Node> = emptyList(),
    val breadcrumb: List<Node> = emptyList(),
    val editingNodeId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para la pantalla principal de outline.
 * 
 * DECISIÓN: Manejamos el "focus" mediante currentNodeId.
 * - null significa que estamos en la raíz (mostramos todos los nodos sin padre)
 * - Un ID específico significa que estamos "focalizados" en esa rama
 * 
 * El breadcrumb se construye dinámicamente desde los ancestros del nodo actual.
 */
@OptIn(FlowPreview::class)
class OutlineViewModel(private val repository: NodeRepository) : ViewModel() {

    private val _currentNodeId = MutableStateFlow<Long?>(null)
    private val _editingNodeId = MutableStateFlow<Long?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OutlineUiState> = combine(
        _currentNodeId,
        _editingNodeId,
        _isLoading,
        _error
    ) { currentId, editingId, loading, error ->
        OutlineUiState(
            currentNodeId = currentId,
            editingNodeId = editingId,
            isLoading = loading,
            error = error
        )
    }.flatMapLatest { state ->
        // Cargar nodos hijos del nodo actual
        val nodesFlow = if (state.currentNodeId == null) {
            repository.getRootNodes()
        } else {
            repository.getChildren(state.currentNodeId)
        }

        // Cargar breadcrumb
        val breadcrumbFlow = flow {
            val breadcrumb = if (state.currentNodeId != null) {
                repository.getBreadcrumb(state.currentNodeId)
            } else {
                emptyList()
            }
            emit(breadcrumb)
        }

        combine(nodesFlow, breadcrumbFlow) { nodes, breadcrumb ->
            state.copy(
                nodes = nodes,
                breadcrumb = breadcrumb
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OutlineUiState()
    )

    // ===== NAVEGACIÓN Y FOCUS =====

    /**
     * Entra en "focus mode" para un nodo específico.
     * Muestra solo los hijos de ese nodo.
     */
    fun focusOnNode(nodeId: Long) {
        _currentNodeId.value = nodeId
    }

    /**
     * Navega a un ancestro específico del breadcrumb.
     */
    fun navigateToAncestor(ancestorId: Long?) {
        _currentNodeId.value = ancestorId
    }

    /**
     * Vuelve al nivel anterior (padre del nodo actual).
     */
    fun navigateUp() {
        viewModelScope.launch {
            val currentId = _currentNodeId.value ?: return@launch
            val parent = repository.getNode(currentId)?.parentId
            _currentNodeId.value = parent
        }
    }

    /**
     * Vuelve a la raíz.
     */
    fun navigateToRoot() {
        _currentNodeId.value = null
    }

    // ===== EDICIÓN =====

    fun startEditing(nodeId: Long) {
        _editingNodeId.value = nodeId
    }

    fun stopEditing() {
        _editingNodeId.value = null
    }

    fun updateNodeTitle(nodeId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(nodeId, newTitle)
        }
    }

    // ===== CREACIÓN =====

    /**
     * Crea un nuevo nodo hermano después del nodo dado.
     * Equivalente a "Enter" en WorkFlowy.
     */
    fun createSiblingAfter(node: Node, title: String = "") {
        viewModelScope.launch {
            val newId = repository.createSibling(node, title)
            // Auto-empezar a editar el nuevo nodo
            _editingNodeId.value = newId
        }
    }

    /**
     * Crea un nuevo nodo hijo del nodo dado.
     * Equivalente a "Tab" (indent) en un nuevo nodo.
     */
    fun createChild(parentId: Long, title: String = "") {
        viewModelScope.launch {
            val newId = repository.createChild(parentId, title)
            _editingNodeId.value = newId
        }
    }

    /**
     * Crea un nodo al final de la lista actual.
     */
    fun createNodeAtEnd(title: String = "") {
        viewModelScope.launch {
            val parentId = _currentNodeId.value
            val newId = repository.createChild(parentId, title)
            _editingNodeId.value = newId
        }
    }

    // ===== OPERACIONES DE ÁRBOL =====

    fun indent(node: Node) {
        viewModelScope.launch {
            repository.indent(node)
        }
    }

    fun outdent(node: Node) {
        viewModelScope.launch {
            repository.outdent(node)
        }
    }

    fun moveUp(node: Node) {
        viewModelScope.launch {
            repository.moveUp(node)
        }
    }

    fun moveDown(node: Node) {
        viewModelScope.launch {
            repository.moveDown(node)
        }
    }

    fun deleteNode(nodeId: Long) {
        viewModelScope.launch {
            repository.deleteNode(nodeId)
            if (_editingNodeId.value == nodeId) {
                _editingNodeId.value = null
            }
        }
    }

    fun toggleCollapsed(nodeId: Long) {
        viewModelScope.launch {
            repository.toggleCollapsed(nodeId)
        }
    }

    // ===== UTILIDADES =====

    fun getCurrentParentId(): Long? = _currentNodeId.value

    companion object {
        fun provideFactory(repository: NodeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OutlineViewModel(repository) as T
                }
            }
        }
    }
}
