package com.ma.app.data.repository

import com.ma.app.data.database.NodeDao
import com.ma.app.data.model.Node
import com.ma.app.data.model.NodeType
import com.ma.app.data.model.NodeWithPath
import com.ma.app.data.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class NodeRepository(private val nodeDao: NodeDao) {

    // ===== OPERACIONES BÁSICAS =====

    fun getNodeById(id: Long): Flow<Node?> = nodeDao.getByIdFlow(id)

    suspend fun getNode(id: Long): Node? = nodeDao.getById(id)

    fun getRootNodes(): Flow<List<Node>> = nodeDao.getRootNodes()

    fun getChildren(parentId: Long): Flow<List<Node>> = nodeDao.getChildren(parentId)

    suspend fun getChildrenSync(parentId: Long): List<Node> = nodeDao.getChildrenSync(parentId)

    suspend fun getChildrenCount(parentId: Long): Int = nodeDao.getChildrenCount(parentId)

    // ===== TAREAS (TODOIST STYLE) =====

    fun getPendingTasks(): Flow<List<Node>> = nodeDao.getPendingTasks()

    fun getCompletedTasks(): Flow<List<Node>> = nodeDao.getCompletedTasks()

    fun getAllTasks(): Flow<List<Node>> = nodeDao.getAllTasks()

    fun getOverdueTasks(): Flow<List<Node>> = nodeDao.getOverdueTasks(Date())

    fun getTasksForDay(date: Date): Flow<List<Node>> {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.time
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.time
        return nodeDao.getTasksForDay(startOfDay, endOfDay)
    }

    suspend fun toggleTaskCompleted(nodeId: Long) {
        val node = nodeDao.getById(nodeId) ?: return
        val now = Date()
        val completed = !node.isCompleted
        nodeDao.updateCompleted(nodeId, completed, if (completed) now else null, now)
    }

    suspend fun setTaskPriority(nodeId: Long, priority: Priority) {
        nodeDao.updatePriority(nodeId, priority.name, Date())
    }

    suspend fun setDueDate(nodeId: Long, dueDate: Date?) {
        nodeDao.updateDueDate(nodeId, dueDate, Date())
    }

    suspend fun setNodeType(nodeId: Long, nodeType: NodeType) {
        nodeDao.updateNodeType(nodeId, nodeType.name, Date())
    }

    suspend fun createTask(parentId: Long? = null, title: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val orderIndex = nodeDao.getNextOrderIndex(parentId)
            val newNode = Node(
                parentId = parentId,
                orderIndex = orderIndex,
                title = title,
                nodeType = NodeType.TASK.name,
                createdAt = Date(),
                updatedAt = Date()
            )
            nodeDao.insert(newNode)
        }
    }

    // ===== CALENDARIO =====

    fun getNodesInDateRange(start: Date, end: Date): Flow<List<Node>> =
        nodeDao.getNodesInDateRange(start, end)

    suspend fun getDatesWithNodes(start: Date, end: Date): List<String> =
        nodeDao.getDatesWithNodes(start, end)

    // ===== CREACIÓN =====

    suspend fun createSibling(afterNode: Node, title: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(afterNode.parentId)
            val afterIndex = siblings.indexOfFirst { it.id == afterNode.id }

            val newOrderIndex = if (afterIndex < siblings.size - 1) {
                val nextNode = siblings[afterIndex + 1]
                (afterNode.orderIndex + nextNode.orderIndex) / 2.0
            } else {
                afterNode.orderIndex + 1.0
            }

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
                nodeType = afterNode.nodeType, // hereda el tipo del padre
                createdAt = Date(),
                updatedAt = Date()
            )
            nodeDao.insert(newNode)
        }
    }

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

    suspend fun indent(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }
            if (currentIndex <= 0) return@withContext false

            val newParent = siblings[currentIndex - 1]
            val lastChild = nodeDao.getLastChild(newParent.id)
            val newOrderIndex = (lastChild?.orderIndex ?: 0.0) + 1.0

            nodeDao.update(
                node.copy(parentId = newParent.id, orderIndex = newOrderIndex, updatedAt = Date())
            )
            true
        }
    }

    suspend fun outdent(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val parentId = node.parentId ?: return@withContext false
            val parent = nodeDao.getById(parentId) ?: return@withContext false
            val newParentId = parent.parentId
            val siblings = nodeDao.getSiblings(newParentId)
            val parentIndex = siblings.indexOfFirst { it.id == parent.id }

            val newOrderIndex = if (parentIndex < siblings.size - 1) {
                val nextSibling = siblings[parentIndex + 1]
                (parent.orderIndex + nextSibling.orderIndex) / 2.0
            } else {
                parent.orderIndex + 1.0
            }

            nodeDao.update(
                node.copy(parentId = newParentId, orderIndex = newOrderIndex, updatedAt = Date())
            )
            true
        }
    }

    // ===== REORDENAMIENTO =====

    suspend fun moveUp(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }
            if (currentIndex <= 0) return@withContext false

            val prevNode = siblings[currentIndex - 1]
            val tempOrder = prevNode.orderIndex
            nodeDao.update(prevNode.copy(orderIndex = node.orderIndex, updatedAt = Date()))
            nodeDao.update(node.copy(orderIndex = tempOrder, updatedAt = Date()))
            true
        }
    }

    suspend fun moveDown(node: Node): Boolean {
        return withContext(Dispatchers.IO) {
            val siblings = nodeDao.getSiblings(node.parentId)
            val currentIndex = siblings.indexOfFirst { it.id == node.id }
            if (currentIndex >= siblings.size - 1) return@withContext false

            val nextNode = siblings[currentIndex + 1]
            val tempOrder = nextNode.orderIndex
            nodeDao.update(nextNode.copy(orderIndex = node.orderIndex, updatedAt = Date()))
            nodeDao.update(node.copy(orderIndex = tempOrder, updatedAt = Date()))
            true
        }
    }

    suspend fun moveToParent(node: Node, newParentId: Long?, newOrderIndex: Double) {
        nodeDao.update(
            node.copy(parentId = newParentId, orderIndex = newOrderIndex, updatedAt = Date())
        )
    }

    // ===== BÚSQUEDA =====

    fun searchWithContext(query: String): Flow<List<NodeWithPath>> = flow {
        val results = withContext(Dispatchers.IO) {
            if (query.isBlank()) emptyList()
            else {
                nodeDao.searchNodes(query).map { node ->
                    NodeWithPath(node = node, path = buildAncestorPath(node))
                }
            }
        }
        emit(results)
    }.flowOn(Dispatchers.IO)

    fun searchByTag(tag: String): Flow<List<NodeWithPath>> = flow {
        val results = withContext(Dispatchers.IO) {
            val searchTag = if (tag.startsWith("#")) tag else "#$tag"
            nodeDao.getAllNodes().filter { node ->
                node.title.contains(searchTag) || node.note?.contains(searchTag) == true
            }.map { node ->
                NodeWithPath(node = node, path = buildAncestorPath(node))
            }
        }
        emit(results)
    }.flowOn(Dispatchers.IO)

    // ===== ANCESTROS =====

    suspend fun buildAncestorPath(node: Node): List<Node> {
        return withContext(Dispatchers.IO) {
            val path = mutableListOf<Node>()
            var currentParentId = node.parentId
            while (currentParentId != null) {
                val parent = nodeDao.getById(currentParentId) ?: break
                path.add(0, parent)
                currentParentId = parent.parentId
            }
            path
        }
    }

    suspend fun getBreadcrumb(nodeId: Long): List<Node> {
        return withContext(Dispatchers.IO) {
            val node = nodeDao.getById(nodeId) ?: return@withContext emptyList()
            buildAncestorPath(node)
        }
    }

    // ===== LINKS INTERNOS =====

    suspend fun findNodeByTitle(title: String): Node? = nodeDao.findByTitle(title)

    suspend fun searchNodesByTitle(query: String): List<Node> = nodeDao.searchByTitle(query)

    // ===== EXPORT / BACKUP =====

    suspend fun getAllNodes(): List<Node> = nodeDao.getAllNodes()

    suspend fun importNodes(nodes: List<Node>) {
        nodeDao.deleteAll()
        nodeDao.insertAll(nodes)
    }

    // ===== PRIVADOS =====

    private fun shouldReindex(siblings: List<Node>): Boolean {
        if (siblings.size < 2) return false
        for (i in 0 until siblings.size - 1) {
            if (siblings[i + 1].orderIndex - siblings[i].orderIndex < 0.0001) return true
        }
        return false
    }

    private suspend fun reindexSiblings(parentId: Long?) {
        val siblings = nodeDao.getSiblings(parentId).sortedBy { it.orderIndex }
        nodeDao.updateAll(siblings.mapIndexed { i, n -> n.copy(orderIndex = (i + 1).toDouble()) })
    }
}
