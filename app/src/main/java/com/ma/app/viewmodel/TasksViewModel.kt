package com.ma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.Node
import com.ma.app.data.model.NodeType
import com.ma.app.data.model.Priority
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class TasksUiState(
    val pendingTasks: List<Node> = emptyList(),
    val completedTasks: List<Node> = emptyList(),
    val overdueTasks: List<Node> = emptyList(),
    val showCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class TasksViewModel(private val repository: NodeRepository) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getPendingTasks(),
        repository.getCompletedTasks(),
        repository.getOverdueTasks(),
        _showCompleted,
        _isLoading
    ) { pending, completed, overdue, showCompleted, isLoading ->
        TasksUiState(
            pendingTasks = pending,
            completedTasks = completed,
            overdueTasks = overdue,
            showCompleted = showCompleted,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(isLoading = true)
    )

    fun createTask(title: String) {
        viewModelScope.launch {
            repository.createTask(title = title)
        }
    }

    fun toggleCompleted(nodeId: Long) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(nodeId)
        }
    }

    fun setPriority(nodeId: Long, priority: Priority) {
        viewModelScope.launch {
            repository.setTaskPriority(nodeId, priority)
        }
    }

    fun setDueDate(nodeId: Long, dueDate: Date?) {
        viewModelScope.launch {
            repository.setDueDate(nodeId, dueDate)
        }
    }

    fun deleteTask(nodeId: Long) {
        viewModelScope.launch {
            repository.deleteNode(nodeId)
        }
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    companion object {
        fun provideFactory(repository: NodeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasksViewModel(repository) as T
                }
            }
        }
    }
}
