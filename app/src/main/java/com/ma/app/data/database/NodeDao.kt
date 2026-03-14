package com.ma.app.data.database

import androidx.room.*
import com.ma.app.data.model.Node
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface NodeDao {

    // ===== QUERIES BÁSICAS =====

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getById(id: Long): Node?

    @Query("SELECT * FROM nodes WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Node?>

    @Query("SELECT * FROM nodes ORDER BY id DESC LIMIT 1")
    suspend fun getLastInserted(): Node?

    // ===== QUERIES JERÁRQUICAS =====

    @Query("SELECT * FROM nodes WHERE parentId IS NULL ORDER BY orderIndex ASC")
    fun getRootNodes(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE parentId = :parentId ORDER BY orderIndex ASC")
    fun getChildren(parentId: Long): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE parentId = :parentId ORDER BY orderIndex ASC")
    suspend fun getChildrenSync(parentId: Long): List<Node>

    @Query("SELECT * FROM nodes WHERE parentId IS NULL ORDER BY orderIndex ASC")
    suspend fun getRootNodesSync(): List<Node>

    @Query("SELECT COUNT(*) FROM nodes WHERE parentId = :parentId")
    suspend fun getChildrenCount(parentId: Long): Int

    @Query("SELECT * FROM nodes WHERE parentId = :parentId ORDER BY orderIndex DESC LIMIT 1")
    suspend fun getLastChild(parentId: Long): Node?

    // ===== QUERIES DE TAREAS (TODOIST STYLE) =====

    @Query("SELECT * FROM nodes WHERE nodeType = 'TASK' AND isCompleted = 0 ORDER BY priority DESC, dueDate ASC NULLS LAST, orderIndex ASC")
    fun getPendingTasks(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE nodeType = 'TASK' AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE nodeType = 'TASK' ORDER BY isCompleted ASC, priority DESC, dueDate ASC NULLS LAST")
    fun getAllTasks(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE nodeType = 'TASK' AND isCompleted = 0 AND dueDate IS NOT NULL AND dueDate <= :today ORDER BY dueDate ASC")
    fun getOverdueTasks(today: Date): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE nodeType = 'TASK' AND isCompleted = 0 AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY orderIndex ASC")
    fun getTasksForDay(startOfDay: Date, endOfDay: Date): Flow<List<Node>>

    @Query("UPDATE nodes SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: Long, completed: Boolean, completedAt: Date?, updatedAt: Date)

    @Query("UPDATE nodes SET priority = :priority, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: String, updatedAt: Date)

    @Query("UPDATE nodes SET dueDate = :dueDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: Date?, updatedAt: Date)

    // ===== QUERIES DE CALENDARIO =====

    @Query("SELECT * FROM nodes WHERE (nodeType = 'TASK' OR nodeType = 'EVENT') AND dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    fun getNodesInDateRange(start: Date, end: Date): Flow<List<Node>>

    @Query("SELECT DISTINCT date(dueDate / 1000, 'unixepoch') FROM nodes WHERE dueDate BETWEEN :start AND :end AND dueDate IS NOT NULL")
    suspend fun getDatesWithNodes(start: Date, end: Date): List<String>

    // ===== QUERIES DE BÚSQUEDA =====

    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun searchNodes(query: String): List<Node>

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodes(): List<Node>

    // ===== QUERIES PARA ANCESTROS =====

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getParent(id: Long): Node?

    // ===== OPERACIONES CRUD =====

    @Insert
    suspend fun insert(node: Node): Long

    @Insert
    suspend fun insertAll(nodes: List<Node>): List<Long>

    @Update
    suspend fun update(node: Node)

    @Update
    suspend fun updateAll(nodes: List<Node>)

    @Delete
    suspend fun delete(node: Node)

    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM nodes")
    suspend fun deleteAll()

    // ===== OPERACIONES ESPECÍFICAS =====

    @Query("SELECT COALESCE(MAX(orderIndex), 0) + 1.0 FROM nodes WHERE parentId = :parentId")
    suspend fun getNextOrderIndex(parentId: Long?): Double

    @Query("SELECT * FROM nodes WHERE parentId = :parentId OR (parentId IS NULL AND :parentId IS NULL) ORDER BY orderIndex ASC")
    suspend fun getSiblings(parentId: Long?): List<Node>

    @Query("SELECT * FROM nodes WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): Node?

    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchByTitle(query: String): List<Node>

    @Query("UPDATE nodes SET nodeType = :nodeType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNodeType(id: Long, nodeType: String, updatedAt: Date)
}
