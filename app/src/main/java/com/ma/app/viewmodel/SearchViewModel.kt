package com.ma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.NodeWithPath
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado UI para la pantalla de búsqueda.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<NodeWithPath> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val selectedTag: String? = null
)

/**
 * ViewModel para búsqueda con contexto jerárquico.
 * 
 * DECISIÓN: Usamos debounce en la búsqueda para no saturar la DB
 * con cada pulsación de tecla. 300ms es un buen balance entre
 * responsividad y performance.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: NodeRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isSearching = MutableStateFlow(false)
    private val _hasSearched = MutableStateFlow(false)

    // Debounce de 300ms para la búsqueda
    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _selectedTag,
        _isSearching,
        _hasSearched
    ) { query, tag, searching, hasSearched ->
        SearchUiState(
            query = query,
            selectedTag = tag,
            isSearching = searching,
            hasSearched = hasSearched
        )
    }.flatMapLatest { state ->
        if (state.query.isBlank() && state.selectedTag == null) {
            flowOf(state.copy(results = emptyList()))
        } else {
            _isSearching.value = true

            val resultsFlow = if (state.selectedTag != null) {
                repository.searchByTag(state.selectedTag)
            } else {
                repository.searchWithContext(state.query)
            }

            resultsFlow.map { results ->
                _isSearching.value = false
                _hasSearched.value = true
                state.copy(results = results)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    // ===== BÚSQUEDA =====

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _hasSearched.value = false
        }
    }

    fun clearSearch() {
        _query.value = ""
        _selectedTag.value = null
        _hasSearched.value = false
    }

    // ===== FILTRO POR TAG =====

    fun searchByTag(tag: String) {
        _selectedTag.value = tag
        _query.value = ""
    }

    fun clearTagFilter() {
        _selectedTag.value = null
    }

    // ===== NAVEGACIÓN DESDE RESULTADOS =====

    /**
     * Obtiene el ID del padre de un nodo para navegar a él.
     */
    suspend fun getParentIdForNavigation(nodeId: Long): Long? {
        val node = repository.getNode(nodeId)
        return node?.parentId
    }

    companion object {
        fun provideFactory(repository: NodeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(repository) as T
                }
            }
        }
    }
}
