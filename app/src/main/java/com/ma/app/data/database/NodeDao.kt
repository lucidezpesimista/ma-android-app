package com.ma.app.data.database

import androidx.room.*
import com.ma.app.data.model.Node
import kotlinx.coroutines.flow.Flow

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

    // ===== QUERIES PARA BÚSQUEDA =====

    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun searchNodes(query: String): List<Node>

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodes(): List<Node>

    // ===== QUERIES PARA ANCESTROS =====

    /**
     * Obtiene todos los ancestros de un nodo (para breadcrumb).
     * Room no soporta CTEs recursivos directamente, así que hacemos esto en código.
     */
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

    /**
     * Obtiene el siguiente orderIndex disponible para un nuevo hijo de parentId.
     * Si no hay hijos, retorna 1.0
     */
    @Query("SELECT COALESCE(MAX(orderIndex), 0) + 1.0 FROM nodes WHERE parentId = :parentId")
    suspend fun getNextOrderIndex(parentId: Long?): Double

    /**
     * Obtiene todos los nodos de un padre específico ordenados.
     * Útil para reordenamiento.
     */
    @Query("SELECT * FROM nodes WHERE parentId = :parentId OR (parentId IS NULL AND :parentId IS NULL) ORDER BY orderIndex ASC")
    suspend fun getSiblings(parentId: Long?): List<Node>

    /**
     * Verifica si existe un nodo con el título dado (para links internos).
     */
    @Query("SELECT * FROM nodes WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): Node?

    /**
     * Busca nodos cuyo título contenga el texto (para autocompletar links).
     */
    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchByTitle(query: String): List<Node>
}
