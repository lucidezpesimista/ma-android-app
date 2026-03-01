package com.ma.app.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parser para hashtags y links internos.
 * 
 * HASHTAGS: #palabra (alfanumérico y guión bajo)
 * LINKS INTERNOS: [[NombreDelNodo]] o [[ID]]
 * 
 * DECISIÓN: Los hashtags son detectables en el texto pero se almacenan como texto plano.
 * No normalizamos a una tabla de tags para el MVP - se computan al vuelo.
 */
object TextParser {

    // Regex para hashtags: # seguido de alfanuméricos y guiones bajos
    private val HASHTAG_REGEX = Regex("#([a-zA-Z0-9_]+)")

    // Regex para links internos: [[contenido]]
    private val INTERNAL_LINK_REGEX = Regex("\\[\\[([^\\]]+)\\]\\]")

    // Colores para el resaltado
    private val HASHTAG_COLOR = Color(0xFF2196F3) // Azul Material
    private val LINK_COLOR = Color(0xFF4CAF50)    // Verde Material

    data class ParsedText(
        val annotatedString: AnnotatedString,
        val hashtags: List<String>,
        val internalLinks: List<String>
    )

    data class TagRange(
        val tag: String,
        val start: Int,
        val end: Int,
        val type: TagType
    )

    enum class TagType {
        HASHTAG,
        INTERNAL_LINK
    }

    /**
     * Parsea texto y extrae hashtags y links con sus posiciones.
     */
    fun parse(text: String): ParsedText {
        val hashtags = mutableListOf<String>()
        val internalLinks = mutableListOf<String>()

        val annotatedString = buildAnnotatedString {
            append(text)

            // Procesar hashtags
            HASHTAG_REGEX.findAll(text).forEach { matchResult ->
                val tag = matchResult.groupValues[1]
                hashtags.add(tag)

                addStyle(
                    style = SpanStyle(
                        color = HASHTAG_COLOR,
                        fontWeight = FontWeight.Medium
                    ),
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )

                // Agregar tag para click handling
                addStringAnnotation(
                    tag = "hashtag",
                    annotation = tag,
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }

            // Procesar links internos
            INTERNAL_LINK_REGEX.findAll(text).forEach { matchResult ->
                val linkContent = matchResult.groupValues[1]
                internalLinks.add(linkContent)

                addStyle(
                    style = SpanStyle(
                        color = LINK_COLOR,
                        fontWeight = FontWeight.Medium
                    ),
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )

                // Agregar tag para click handling
                addStringAnnotation(
                    tag = "internal_link",
                    annotation = linkContent,
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }
        }

        return ParsedText(
            annotatedString = annotatedString,
            hashtags = hashtags.distinct(),
            internalLinks = internalLinks.distinct()
        )
    }

    /**
     * Extrae solo los hashtags de un texto.
     */
    fun extractHashtags(text: String): List<String> {
        return HASHTAG_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    /**
     * Extrae solo los links internos de un texto.
     */
    fun extractInternalLinks(text: String): List<String> {
        return INTERNAL_LINK_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    /**
     * Verifica si un texto contiene algún hashtag.
     */
    fun containsHashtag(text: String, hashtag: String): Boolean {
        val searchTag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
        return text.contains(searchTag)
    }

    /**
     * Crea un AnnotatedString con resaltado para visualización.
     * Versión simplificada para items de lista.
     */
    fun highlightText(text: String): AnnotatedString {
        return parse(text).annotatedString
    }

    /**
     * Obtiene todos los tags (hashtags y links) con sus rangos.
     * Útil para manejar clicks en la UI.
     */
    fun getTagRanges(text: String): List<TagRange> {
        val ranges = mutableListOf<TagRange>()

        HASHTAG_REGEX.findAll(text).forEach { matchResult ->
            ranges.add(
                TagRange(
                    tag = matchResult.groupValues[1],
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1,
                    type = TagType.HASHTAG
                )
            )
        }

        INTERNAL_LINK_REGEX.findAll(text).forEach { matchResult ->
            ranges.add(
                TagRange(
                    tag = matchResult.groupValues[1],
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1,
                    type = TagType.INTERNAL_LINK
                )
            )
        }

        return ranges.sortedBy { it.start }
    }

    /**
     * Encuentra qué tag está en una posición específica del texto.
     * Retorna null si no hay tag en esa posición.
     */
    fun findTagAtPosition(text: String, position: Int): TagRange? {
        return getTagRanges(text).find { position in it.start until it.end }
    }

    /**
     * Convierte un título a formato de link interno.
     */
    fun toInternalLink(title: String): String {
        return "[[$title]]"
    }

    /**
     * Escapa caracteres especiales para evitar parsing accidental.
     * No implementado para MVP - los usuarios pueden usar \# si quieren literal.
     */
    fun escapeText(text: String): String {
        // Por ahora, retornamos el texto tal cual
        // En el futuro podríamos escapar # y [[ si el usuario quiere texto literal
        return text
    }
}
