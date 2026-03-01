package com.ma.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad principal del outliner.
 * 
 * DECISIÓN CLAVE - orderIndex como Double (fractional indexing):
 * Usamos Double en lugar de Long para orderIndex. Esto permite insertar ítems entre
 * dos existentes sin necesidad de reindexar toda la lista. Por ejemplo, si tenemos
 * ítems con orderIndex 1.0 y 2.0, podemos insertar uno nuevo con 1.5.
 * 
 * Cuando el espacio entre ítems se vuelve muy pequeño (ej: 1.0 y 1.000001),
 * eventualmente necesitaremos reindexar, pero esto es raro en uso normal.
 * 
 * Un nodo puede tener:
 * - title: el texto principal del ítem (una línea preferiblemente)
 * - note: texto adicional/descripción (puede ser multi-línea)
 * - parentId: null para raíz, o ID del padre
 * - orderIndex: para ordenar hermanos
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
        Index(value = ["parentId", "orderIndex"])
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
    val isCollapsed: Boolean = false
) {
    /**
     * Retorna true si este nodo es root (sin padre)
     */
    fun isRoot(): Boolean = parentId == null

    /**
     * Crea una copia con timestamp actualizado
     */
    fun withUpdatedTimestamp(): Node = copy(updatedAt = Date())
}

/**
 * Data class para representar un nodo con su información de jerarquía
 * usado en búsqueda para mostrar contexto.
 */
data class NodeWithPath(
    val node: Node,
    val path: List<Node>, // Ancestros desde root hasta el padre
    val childrenCount: Int = 0
) {
    /**
     * Retorna el path como string formateado: "Root > Parent > ..."
     */
    fun pathString(): String {
        return path.map { it.title.takeIf { t -> t.isNotBlank() } ?: "(sin título)" }
            .plus(node.title.takeIf { it.isNotBlank() } ?: "(sin título)")
            .joinToString(" > ")
    }
}

/**
 * Representa un nodo con sus hijos inmediatos (para la UI de outline)
 */
data class NodeWithChildren(
    val node: Node,
    val children: List<Node> = emptyList(),
    val hasMoreChildren: Boolean = false
)
