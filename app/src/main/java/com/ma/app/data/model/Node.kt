package com.ma.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Tipos de nodo para diferenciar entre notas, tareas y eventos.
 */
enum class NodeType {
    NOTE,   // Nota de texto libre (estilo Workflowy)
    TASK,   // Tarea con checkbox, prioridad y fecha límite (estilo Todoist)
    EVENT   // Evento con fecha en el calendario
}

/**
 * Niveles de prioridad para tareas.
 */
enum class Priority {
    NONE, LOW, MEDIUM, HIGH
}

/**
 * Entidad principal del outliner.
 *
 * DECISIÓN CLAVE - orderIndex como Double (fractional indexing):
 * Permite insertar ítems entre dos existentes sin reindexar toda la lista.
 *
 * Campos nuevos para tareas y calendario:
 * - nodeType: diferencia notas, tareas y eventos
 * - isCompleted: para tareas
 * - dueDate: fecha límite (tareas) o fecha de evento
 * - priority: prioridad de la tarea
 * - completedAt: cuándo se completó
 */
@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = Node::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["parentId", "orderIndex"]),
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["nodeType"])
    ]
)
data class Node(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentId: Long? = null,
    val orderIndex: Double = 0.0,
    val title: String = "",
    val note: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isCollapsed: Boolean = false,

    // Campos de tarea/evento
    val nodeType: String = NodeType.NOTE.name,
    val isCompleted: Boolean = false,
    val dueDate: Date? = null,
    val priority: String = Priority.NONE.name,
    val completedAt: Date? = null
) {
    fun isRoot(): Boolean = parentId == null
    fun withUpdatedTimestamp(): Node = copy(updatedAt = Date())
    fun nodeTypeEnum(): NodeType = NodeType.valueOf(nodeType)
    fun priorityEnum(): Priority = Priority.valueOf(priority)
}

/**
 * Data class para representar un nodo con su información de jerarquía.
 */
data class NodeWithPath(
    val node: Node,
    val path: List<Node>,
    val childrenCount: Int = 0
) {
    fun pathString(): String {
        return path.map { it.title.takeIf { t -> t.isNotBlank() } ?: "(sin título)" }
            .plus(node.title.takeIf { it.isNotBlank() } ?: "(sin título)")
            .joinToString(" > ")
    }
}

/**
 * Representa un nodo con sus hijos inmediatos (para la UI de outline).
 */
data class NodeWithChildren(
    val node: Node,
    val children: List<Node> = emptyList(),
    val hasMoreChildren: Boolean = false
)
