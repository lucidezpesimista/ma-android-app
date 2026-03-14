package com.ma.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.Node
import com.ma.app.data.model.NodeType
import com.ma.app.data.model.Priority
import com.ma.app.data.remote.ClaudeApiService
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class NodeDetailUiState(
    val node: Node? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val claudeResult: String? = null,
    val isClaudeLoading: Boolean = false
)

class NodeDetailViewModel(
    private val repository: NodeRepository,
    private val nodeId: Long,
    private val appContext: Context
) : ViewModel() {

    private val _noteDraft = MutableStateFlow<String?>(null)
    private val _titleDraft = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _claudeResult = MutableStateFlow<String?>(null)
    private val _isClaudeLoading = MutableStateFlow(false)

    private val claudeService = ClaudeApiService()

    // Combinar drafts y estado de Claude en flujos intermedios
    private val _draftsFlow = combine(_noteDraft, _titleDraft) { noteDraft, titleDraft ->
        Pair(noteDraft, titleDraft)
    }

    private val _claudeFlow = combine(_claudeResult, _isClaudeLoading, _error) { result, loading, error ->
        Triple(result, loading, error)
    }

    val uiState: StateFlow<NodeDetailUiState> = combine(
        repository.getNodeById(nodeId),
        _draftsFlow,
        _claudeFlow,
        _isLoading
    ) { node, drafts, claude, isLoading ->
        val noteDraft = drafts.first
        val titleDraft = drafts.second
        val claudeResult = claude.first
        val isClaudeLoading = claude.second
        val error = claude.third

        val hasNoteChanges = noteDraft != null && noteDraft != node?.note
        val hasTitleChanges = titleDraft != null && titleDraft != node?.title

        NodeDetailUiState(
            node = node?.copy(
                note = noteDraft ?: node.note,
                title = titleDraft ?: node.title
            ),
            isLoading = isLoading,
            error = error,
            hasUnsavedChanges = hasNoteChanges || hasTitleChanges,
            claudeResult = claudeResult,
            isClaudeLoading = isClaudeLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NodeDetailUiState(isLoading = true)
    )

    fun updateNote(newNote: String) {
        _noteDraft.value = newNote
    }

    fun saveNote() {
        viewModelScope.launch {
            val draft = _noteDraft.value
            val titleDraft = _titleDraft.value
            if (draft != null) {
                repository.updateNote(nodeId, draft.takeIf { it.isNotBlank() })
                _noteDraft.value = null
            }
            if (titleDraft != null) {
                repository.updateTitle(nodeId, titleDraft)
                _titleDraft.value = null
            }
        }
    }

    fun discardChanges() {
        _noteDraft.value = null
        _titleDraft.value = null
    }

    fun updateTitle(newTitle: String) {
        _titleDraft.value = newTitle
    }

    fun setDueDate(dueDate: Date?) {
        viewModelScope.launch {
            repository.setDueDate(nodeId, dueDate)
        }
    }

    fun setPriority(priority: Priority) {
        viewModelScope.launch {
            repository.setTaskPriority(nodeId, priority)
        }
    }

    fun toggleCompleted() {
        viewModelScope.launch {
            repository.toggleTaskCompleted(nodeId)
        }
    }

    fun convertToTask() {
        viewModelScope.launch {
            repository.setNodeType(nodeId, NodeType.TASK)
        }
    }

    fun convertToNote() {
        viewModelScope.launch {
            repository.setNodeType(nodeId, NodeType.NOTE)
        }
    }

    // ===== CLAUDE AI =====

    private fun getApiKey(): String {
        val prefs = appContext.getSharedPreferences("ma_prefs", Context.MODE_PRIVATE)
        return prefs.getString("claude_api_key", "") ?: ""
    }

    fun spellCheckNote() {
        val note = _noteDraft.value ?: uiState.value.node?.note ?: return
        if (note.isBlank()) return
        val key = getApiKey()
        if (key.isBlank()) {
            _error.value = "Configura tu API key de Claude en Configuración"
            return
        }

        _isClaudeLoading.value = true
        viewModelScope.launch {
            val result = claudeService.spellCheck(key, note)
            _isClaudeLoading.value = false
            result.fold(
                onSuccess = { corrected ->
                    _noteDraft.value = corrected
                    _claudeResult.value = "Texto corregido"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun summarizeNote() {
        val note = _noteDraft.value ?: uiState.value.node?.note ?: return
        if (note.isBlank()) return
        val key = getApiKey()
        if (key.isBlank()) {
            _error.value = "Configura tu API key de Claude en Configuración"
            return
        }

        _isClaudeLoading.value = true
        viewModelScope.launch {
            val result = claudeService.summarize(key, note)
            _isClaudeLoading.value = false
            result.fold(
                onSuccess = { _claudeResult.value = it },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun clearClaudeResult() {
        _claudeResult.value = null
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun provideFactory(
            repository: NodeRepository,
            nodeId: Long,
            context: Context
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NodeDetailViewModel(repository, nodeId, context.applicationContext) as T
                }
            }
        }
    }
}
