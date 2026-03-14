package com.ma.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== REQUEST =====

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

// ===== RESPONSE =====

@Serializable
data class ClaudeResponse(
    val id: String = "",
    val type: String = "",
    val role: String = "",
    val content: List<ClaudeContentBlock> = emptyList(),
    val model: String = "",
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: ClaudeUsage? = null
)

@Serializable
data class ClaudeContentBlock(
    val type: String = "",
    val text: String = ""
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0
)

// ===== ERROR =====

@Serializable
data class ClaudeErrorResponse(
    val type: String = "",
    val error: ClaudeError = ClaudeError()
)

@Serializable
data class ClaudeError(
    val type: String = "",
    val message: String = ""
)

// ===== CHAT HISTORY (para UI) =====

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

enum class ChatRole {
    USER, ASSISTANT
}
