package com.ma.app.data.repository

import com.ma.app.data.database.NodeDao
import com.ma.app.data.model.Node
import com.ma.app.data.model.NodeWithPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date

class NodeRepository(private val nodeDao: NodeDao) {

    // ===== OPERACIONES BÁSICAS =====

    fun getNodeById(id: Long): Flow<Node?> = nodeDao.getByIdFlow(id)

    suspend fun getNode(id: Long): Node? = nodeDao.getById(id)

    fun getRootNodes(): Flow<List<Node>> = nodeDao.getRootNodes()

    fun getChildren(parentId: Long): Flow<List<Node>> = nodeDao.getChildren(parentId)

    suspend fun getChildrenSync(parentId: Long): List<Node> = nodeDao.getChildrenSync(parentId)

    suspend fun getChildrenCount(parentId: Long): Int = nodeDao.getChildrenCount(parentId)

    // ===== CREACIÓN =====

    /**
     * Crea un nuevo nodo hermano después del nodo dado.
     * Retorna el ID del nuevo nodo.
     */
    suspend fun createSibling(afterNode: Node, title: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(afterNode.parentId)
            val afterIndex = siblings.indexOfFirst { it.id == afterNode.id }

            // Calcular nuevo orderIndex entre el nodo actual y el siguiente
            val newOrderIndex = if (afterIndex < siblings.size - 1) {
                val nextNode = siblings[afterIndex + 1]
                (afterNode.orderIndex + nextNode.orderIndex) / 2.0
            } else {
                afterNode.orderIndex + 1.0
            }

            // Si el espacio es muy pequeño, reindexamos
            val finalOrderIndex = if (shouldReindex(siblings)) {
                reindexSiblings(afterNode.parentId)
                afterNode.orderIndex + 1.0
            } else {
                newOrderIndex
            }

            val newNode = Node(
                parentId = afterNode.parentId,
                orderIndex = finalOrderIndex,
                title = title,
                createdAt = Date(),
                updatedAt = Date()
            )
            nodeDao.insert(newNode)
        }
    }

    /**
     * Crea un nuevo nodo al final de los hijos de parentId.
     * Usado para crear primer hijo o agregar al final.
     */
    suspend fun createChild(parentId: Long?, title: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val orderIndex = nodeDao.getNextOrderIndex(parentId)
            val newNode = Node(
                parentId = parentId,
                orderIndex = orderIndex,
                title = title,
                createdAt = Date(),
                updatedAt = Date()
            )
            nodeDao.insert(newNode)
        }
    }

    /**
     * Crea un nodo hijo inmediatamente después de crear un padre.
     * Útil para "Enter" cuando quieres crear hijo del nodo actual.
     */
    suspend fun createChildUnder(parentId: Long, title: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val orderIndex = nodeDao.getNextOrderIndex(parentId)
            val newNode = Node(
                parentId = parentId,
                orderIndex = orderIndex,
                title = title,
                createdAt = Date(),
                updatedAt = Date()
            )
            nodeDao.insert(newNode)
        }
    }

    // ===== ACTUALIZACIÓN =====

    suspend fun updateNode(node: Node) {
        nodeDao.update(node.withUpdatedTimestamp())
    }

    suspend fun updateTitle(nodeId: Long, newTitle: String) {
        val node = nodeDao.getById(nodeId) ?: return
        nodeDao.update(node.copy(title = newTitle, updatedAt = Date()))
    }

    suspend fun updateNote(nodeId: Long, newNote: String?) {
        val node = nodeDao.getById(nodeId) ?: return
        nodeDao.update(node.copy(note = newNote, updatedAt = Date()))
    }

    suspend fun toggleCollapsed(nodeId: Long) {
        val node = nodeDao.getById(nodeId) ?: return
        nodeDao.update(node.copy(isCollapsed = !node.isCollapsed, updatedAt = Date()))
    }

    // ===== ELIMINACIÓN =====

    suspend fun deleteNode(nodeId: Long) {
        nodeDao.deleteById(nodeId)
    }

    // ===== INDENT / OUTDENT =====

    /**
     * Indent: convierte el nodo en hijo del nodo anterior (hermano de arriba).
     * Retorna true si tuvo éxito.
     */
    suspend fun indent(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }

            // No podemos indentar si somos el primer hermano
            if (currentIndex <= 0) return@withContext false

            val newParent = siblings[currentIndex - 1]

            // Mover como último hijo del nuevo padre
            val lastChild = nodeDao.getLastChild(newParent.id)
            val newOrderIndex = (lastChild?.orderIndex ?: 0.0) + 1.0

            nodeDao.update(
                node.copy(
                    parentId = newParent.id,
                    orderIndex = newOrderIndex,
                    updatedAt = Date()
                )
            )
            true
        }
    }

    /**
     * Outdent: convierte el nodo en hermano de su padre.
     * Retorna true si tuvo éxito.
     */
    suspend fun outdent(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val parentId = node.parentId ?: return@withContext false
            val parent = nodeDao.getById(parentId) ?: return@withContext false

            // El nuevo padre es el abuelo
            val newParentId = parent.parentId

            // Calcular orderIndex para estar justo después del padre
            val siblings = nodeDao.getSiblings(newParentId)
            val parentIndex = siblings.indexOfFirst { it.id == parent.id }

            val newOrderIndex = if (parentIndex < siblings.size - 1) {
                val nextSibling = siblings[parentIndex + 1]
                (parent.orderIndex + nextSibling.orderIndex) / 2.0
            } else {
                parent.orderIndex + 1.0
            }

            nodeDao.update(
                node.copy(
                    parentId = newParentId,
                    orderIndex = newOrderIndex,
                    updatedAt = Date()
                )
            )
            true
        }
    }

    // ===== REORDENAMIENTO =====

    /**
     * Mueve el nodo arriba de su hermano anterior.
     */
    suspend fun moveUp(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }

            if (currentIndex <= 0) return@withContext false

            val prevNode = siblings[currentIndex - 1]

            // Intercambiar orderIndex
            val tempOrder = prevNode.orderIndex
            nodeDao.update(prevNode.copy(orderIndex = node.orderIndex, updatedAt = Date()))
            nodeDao.update(node.copy(orderIndex = tempOrder, updatedAt = Date()))

            true
        }
    }

    /**
     * Mueve el nodo abajo de su hermano siguiente.
     */
    suspend fun moveDown(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }

            if (currentIndex >= siblings.size - 1) return@withContext false

            val nextNode = siblings[currentIndex + 1]

            // Intercambiar orderIndex
            val tempOrder = nextNode.orderIndex
            nodeDao.update(nextNode.copy(orderIndex = node.orderIndex, updatedAt = Date()))
            nodeDao.update(node.copy(orderIndex = tempOrder, updatedAt = Date()))

            true
        }
    }

    /**
     * Mueve un nodo a un nuevo padre en una posición específica.
     * Usado para drag & drop.
     */
    suspend fun moveToParent(node: Node, newParentId: Long?, newOrderIndex: Double) {
        nodeDao.update(
            node.copy(
                parentId = newParentId,
                orderIndex = newOrderIndex,
                updatedAt = Date()
            )
        )
    }

    // ===== BÚSQUEDA =====

    /**
     * Búsqueda con contexto jerárquico.
     * Retorna los nodos encontrados con su path de ancestros.
     */
    fun searchWithContext(query: String): Flow<List<NodeWithPath>> = flow {
        val results = withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                emptyList()
            } else {
                val nodes = nodeDao.searchNodes(query)
                nodes.map { node ->
                    NodeWithPath(
                        node = node,
                        path = buildAncestorPath(node)
                    )
                }
            }
        }
        emit(results)
    }.flowOn(Dispatchers.IO)

    /**
     * Busca nodos que contengan un hashtag específico.
     */
    fun searchByTag(tag: String): Flow<List<NodeWithPath>> = flow {
        val results = withContext(Dispatchers.IO) {
            val searchTag = if (tag.startsWith("#")) tag else "#$tag"
            val allNodes = nodeDao.getAllNodes()
            allNodes.filter { node ->
                node.title.contains(searchTag) || node.note?.contains(searchTag) == true
            }.map { node ->
                NodeWithPath(
                    node = node,
                    path = buildAncestorPath(node)
                )
            }
        }
        emit(results)
    }.flowOn(Dispatchers.IO)

    // ===== ANCESTROS Y PATH =====

    /**
     * Construye la lista de ancestros de un nodo (desde root hasta padre).
     */
    suspend fun buildAncestorPath(node: Node): List<Node> {
        return withContext(Dispatchers.IO) {
            val path = mutableListOf<Node>()
            var currentParentId = node.parentId

            while (currentParentId != null) {
                val parent = nodeDao.getById(currentParentId)
                if (parent != null) {
                    path.add(0, parent) // Agregar al inicio
                    currentParentId = parent.parentId
                } else {
                    break
                }
            }
            path
        }
    }

    /**
     * Obtiene el breadcrumb (path de ancestros) para un nodo.
     */
    suspend fun getBreadcrumb(nodeId: Long): List<Node> {
        return withContext(Dispatchers.IO) {
            val node = nodeDao.getById(nodeId) ?: return@withContext emptyList()
            buildAncestorPath(node)
        }
    }

    // ===== LINKS INTERNOS =====

    suspend fun findNodeByTitle(title: String): Node? {
        return nodeDao.findByTitle(title)
    }

    suspend fun searchNodesByTitle(query: String): List<Node> {
        return nodeDao.searchByTitle(query)
    }

    // ===== EXPORT / BACKUP =====

    suspend fun getAllNodes(): List<Node> = nodeDao.getAllNodes()

    suspend fun importNodes(nodes: List<Node>) {
        nodeDao.deleteAll()
        nodeDao.insertAll(nodes)
    }

    // ===== UTILIDADES PRIVADAS =====

    /**
     * Determina si necesitamos reindexar los hermanos.
     * Ocurre cuando el espacio entre índices es muy pequeño.
     */
    private fun shouldReindex(siblings: List<Node>): Boolean {
        if (siblings.size < 2) return false
        for (i in 0 until siblings.size - 1) {
            val diff = siblings[i + 1].orderIndex - siblings[i].orderIndex
            if (diff < 0.0001) return true
        }
        return false
    }

    /**
     * Reindexa todos los hermanos con índices enteros consecutivos.
     */
    private suspend fun reindexSiblings(parentId: Long?) {
        val siblings = nodeDao.getSiblings(parentId).sortedBy { it.orderIndex }
        val updated = siblings.mapIndexed { index, node ->
            node.copy(orderIndex = (index + 1).toDouble())
        }
        nodeDao.updateAll(updated)
    }
}
