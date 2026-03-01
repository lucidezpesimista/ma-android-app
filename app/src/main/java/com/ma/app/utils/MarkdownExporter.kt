package com.ma.app.utils

import com.ma.app.data.model.Node
import com.ma.app.data.repository.NodeRepository

/**
 * Exportador e importador de Markdown.
 * 
 * FORMATO DE EXPORTACIÓN:
 * - Cada nodo se convierte en una línea con guiones según nivel de indentación
 * - La nota se agrega como texto debajo del título, indentada
 * - Hashtags y links se preservan tal cual
 * 
 * Ejemplo:
 * - Proyecto #trabajo
 *   Nota del proyecto
 *   - Subtarea 1
 *   - Subtarea 2
 * - Otro proyecto [[Link interno]]
 */
class MarkdownExporter(private val repository: NodeRepository) {

    /**
     * Exporta todo el árbol a Markdown.
     */
    suspend fun exportToMarkdown(): String {
        val allNodes = repository.getAllNodes()
        val rootNodes = allNodes.filter { it.parentId == null }.sortedBy { it.orderIndex }

        return buildString {
            rootNodes.forEach { node ->
                appendNodeMarkdown(node, allNodes, 0)
            }
        }
    }

    /**
     * Exporta una rama específica a Markdown.
     */
    suspend fun exportBranchToMarkdown(rootNodeId: Long): String {
        val allNodes = repository.getAllNodes()
        val rootNode = allNodes.find { it.id == rootNodeId } ?: return ""

        return buildString {
            appendNodeMarkdown(rootNode, allNodes, 0)
        }
    }

    private fun StringBuilder.appendNodeMarkdown(node: Node, allNodes: List<Node>, level: Int) {
        val indent = "  ".repeat(level)
        val prefix = "- "

        // Línea del título
        appendLine("$indent$prefix${node.title}")

        // Nota si existe
        node.note?.let { note ->
            if (note.isNotBlank()) {
                val noteIndent = "  ".repeat(level + 1)
                note.lines().forEach { line ->
                    appendLine("$noteIndent$line")
                }
            }
        }

        // Hijos recursivamente
        val children = allNodes
            .filter { it.parentId == node.id }
            .sortedBy { it.orderIndex }

        children.forEach { child ->
            appendNodeMarkdown(child, allNodes, level + 1)
        }
    }

    /**
     * Importa desde Markdown.
     * 
     * DECISIÓN: El parsing de Markdown es simplificado para el MVP.
     * Soporta:
     * - Líneas que empiezan con "- " o "* " como nodos
     * - Indentación con 2 espacios o tab
     * - Líneas sin prefijo se consideran continuación de la nota del nodo anterior
     */
    suspend fun importFromMarkdown(markdown: String): List<Node> {
        val lines = markdown.lines()
        val nodes = mutableListOf<Node>()
        val stack = mutableListOf<Pair<Int, Long?>>() // (level, parentId)
        stack.add(-1 to null) // Root

        var currentNoteBuilder = StringBuilder()
        var lastNode: Node? = null

        lines.forEach { line ->
            val (level, content) = parseMarkdownLine(line)

            if (content != null) {
                // Es un nodo nuevo

                // Guardar nota del nodo anterior si existe
                lastNode?.let { last ->
                    val note = currentNoteBuilder.toString().trim()
                    if (note.isNotBlank()) {
                        val updatedLast = last.copy(note = note)
                        val index = nodes.indexOfLast { it.id == last.id }
                        if (index >= 0) {
                            nodes[index] = updatedLast
                        }
                    }
                }
                currentNoteBuilder = StringBuilder()

                // Encontrar padre según nivel
                while (stack.isNotEmpty() && stack.last().first >= level) {
                    stack.removeLast()
                }
                val parentId = stack.lastOrNull()?.second

                // Crear nodo temporal (ID se asignará al insertar)
                val node = Node(
                    id = -(nodes.size + 1).toLong(), // ID temporal negativo
                    parentId = parentId,
                    orderIndex = (nodes.count { it.parentId == parentId } + 1).toDouble(),
                    title = content
                )

                nodes.add(node)
                lastNode = node
                stack.add(level to node.id)
            } else if (line.isNotBlank() && lastNode != null) {
                // Es continuación de nota
                currentNoteBuilder.appendLine(line.trim())
            }
        }

        // Guardar nota del último nodo
        lastNode?.let { last ->
            val note = currentNoteBuilder.toString().trim()
            if (note.isNotBlank()) {
                val updatedLast = last.copy(note = note)
                val index = nodes.indexOfLast { it.id == last.id }
                if (index >= 0) {
                    nodes[index] = updatedLast
                }
            }
        }

        return nodes
    }

    /**
     * Parsea una línea de Markdown y retorna (nivel, contenido).
     * Contenido es null si no es una línea de nodo.
     */
    private fun parseMarkdownLine(line: String): Pair<Int, String?> {
        var trimmed = line
        var level = 0

        // Contar indentación (2 espacios = 1 nivel, o tab = 1 nivel)
        while (trimmed.startsWith("  ") || trimmed.startsWith("\t")) {
            if (trimmed.startsWith("  ")) {
                trimmed = trimmed.drop(2)
            } else {
                trimmed = trimmed.drop(1)
            }
            level++
        }

        // Verificar si es línea de nodo
        return when {
            trimmed.startsWith("- ") -> level to trimmed.drop(2).trim()
            trimmed.startsWith("* ") -> level to trimmed.drop(2).trim()
            trimmed.startsWith("+ ") -> level to trimmed.drop(2).trim()
            else -> level to null
        }
    }

    /**
     * Versión simplificada de exportación para backup rápido.
     * Incluye metadata en el header.
     */
    suspend fun exportForBackup(): String {
        val markdown = exportToMarkdown()
        return buildString {
            appendLine("# Backup Ma Outliner")
            appendLine("# Generated: ${java.util.Date()}")
            appendLine()
            append(markdown)
        }
    }
}
