package com.ma.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.remote.ChatMessage
import com.ma.app.data.remote.ChatRole
import com.ma.app.data.remote.ClaudeApiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClaudeUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val error: String? = null
)

class ClaudeViewModel(
    private val claudeService: ClaudeApiService,
    private val appContext: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _inputText = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _apiKey = MutableStateFlow(loadApiKey())

    val uiState: StateFlow<ClaudeUiState> = combine(
        _messages,
        _inputText,
        _isLoading,
        _error,
        _apiKey
    ) { messages, input, loading, error, key ->
        ClaudeUiState(
            messages = messages,
            inputText = input,
            isLoading = loading,
            apiKey = key,
            hasApiKey = key.isNotBlank(),
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClaudeUiState(apiKey = _apiKey.value, hasApiKey = _apiKey.value.isNotBlank())
    )

    fun updateInput(text: String) {
        _inputText.value = text
        _error.value = null
    }

    fun sendMessage(userMessage: String = _inputText.value.trim()) {
        if (userMessage.isBlank()) return
        val key = _apiKey.value
        if (key.isBlank()) {
            _error.value = "Configura tu API key de Claude en Configuración"
            return
        }

        _inputText.value = ""
        _isLoading.value = true

        // Añadir mensaje del usuario
        _messages.value = _messages.value + ChatMessage(role = ChatRole.USER, content = userMessage)

        // Añadir placeholder de loading para el asistente
        _messages.value = _messages.value + ChatMessage(role = ChatRole.ASSISTANT, content = "", isLoading = true)

        viewModelScope.launch {
            val result = claudeService.sendMessage(apiKey = key, userMessage = userMessage)
            _isLoading.value = false

            // Reemplazar placeholder con respuesta real
            val currentMessages = _messages.value.dropLast(1)
            result.fold(
                onSuccess = { response ->
                    _messages.value = currentMessages + ChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = response
                    )
                },
                onFailure = { error ->
                    _messages.value = currentMessages + ChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = "Error: ${error.message}",
                        isError = true
                    )
                    _error.value = error.message
                }
            )
        }
    }

    fun spellCheck(text: String, onResult: (String) -> Unit) {
        val key = _apiKey.value
        if (key.isBlank()) {
            _error.value = "Configura tu API key de Claude en Configuración"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = claudeService.spellCheck(key, text)
            _isLoading.value = false
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun summarize(text: String, onResult: (String) -> Unit) {
        val key = _apiKey.value
        if (key.isBlank()) {
            _error.value = "Configura tu API key de Claude en Configuración"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = claudeService.summarize(key, text)
            _isLoading.value = false
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun saveApiKey(key: String) {
        _apiKey.value = key
        persistApiKey(key)
        _error.value = null
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    private fun loadApiKey(): String {
        val prefs = appContext.getSharedPreferences("ma_prefs", Context.MODE_PRIVATE)
        return prefs.getString("claude_api_key", "") ?: ""
    }

    private fun persistApiKey(key: String) {
        val prefs = appContext.getSharedPreferences("ma_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("claude_api_key", key).apply()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClaudeViewModel(ClaudeApiService(), context.applicationContext) as T
                }
            }
        }
    }
}
