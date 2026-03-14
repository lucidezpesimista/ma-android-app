package com.ma.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Servicio para comunicarse con la API de Claude (Anthropic).
 *
 * Soporta:
 * - Corrección ortográfica/gramatical
 * - Preguntas generales en contexto de notas
 * - Resumen de contenido
 * - Mejora de texto
 */
class ClaudeApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        const val DEFAULT_MODEL = "claude-sonnet-4-6"
        const val ANTHROPIC_VERSION = "2023-06-01"
        val DEFAULT_SYSTEM_PROMPT = """
            Eres un asistente personal inteligente integrado en una app de notas y tareas llamada "間 (ma)".

            Tu objetivo es ayudar al usuario con:
            - Corrección ortográfica y gramatical en español
            - Responder preguntas sobre el contenido de sus notas
            - Sugerencias de organización y productividad
            - Resúmenes y mejoras de texto

            Siempre responde en español, de forma concisa y útil.
            Si el usuario comparte notas o tareas, úsalas como contexto para dar respuestas más relevantes.
        """.trimIndent()
    }

    /**
     * Envía un mensaje a Claude y retorna la respuesta.
     * @param apiKey Clave de API de Anthropic
     * @param userMessage Mensaje del usuario
     * @param systemPrompt Instrucción de sistema opcional
     */
    suspend fun sendMessage(
        apiKey: String,
        userMessage: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = ClaudeRequest(
                model = DEFAULT_MODEL,
                maxTokens = 1024,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = userMessage)
                )
            )

            val bodyJson = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(CLAUDE_API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val error = try {
                    json.decodeFromString<ClaudeErrorResponse>(responseBody)
                } catch (e: Exception) {
                    null
                }
                return@withContext Result.failure(
                    Exception(error?.error?.message ?: "Error ${response.code}: $responseBody")
                )
            }

            val claudeResponse = json.decodeFromString<ClaudeResponse>(responseBody)
            val text = claudeResponse.content.firstOrNull()?.text ?: ""
            Result.success(text)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corrección ortográfica y gramatical en español.
     */
    suspend fun spellCheck(apiKey: String, text: String): Result<String> {
        return sendMessage(
            apiKey = apiKey,
            userMessage = "Corrige la ortografía y gramática del siguiente texto. Responde SOLO con el texto corregido, sin explicaciones:\n\n$text",
            systemPrompt = "Eres un corrector ortográfico y gramatical experto en español. Tu única tarea es corregir errores ortográficos y gramaticales. Mantén el estilo y tono del texto original. Responde únicamente con el texto corregido."
        )
    }

    /**
     * Resumen de una nota larga.
     */
    suspend fun summarize(apiKey: String, text: String): Result<String> {
        return sendMessage(
            apiKey = apiKey,
            userMessage = "Resume el siguiente texto de forma concisa:\n\n$text",
            systemPrompt = "Eres un asistente que ayuda a resumir notas y textos en español. Crea resúmenes claros, concisos y útiles."
        )
    }

    /**
     * Mejora y expande el texto dado.
     */
    suspend fun enhance(apiKey: String, text: String): Result<String> {
        return sendMessage(
            apiKey = apiKey,
            userMessage = "Mejora y expande el siguiente texto manteniendo las ideas principales:\n\n$text",
            systemPrompt = "Eres un asistente de escritura experto en español. Mejora el texto haciéndolo más claro, coherente y bien estructurado, sin cambiar las ideas principales."
        )
    }

}
