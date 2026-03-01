package com.ma.app

import com.ma.app.data.model.Node
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para operaciones de árbol y lógica de orderIndex.
 */
class TreeOperationsTest {

    @Test
    fun `fractional indexing allows insertion between nodes`() {
        // Simular dos nodos con orderIndex 1.0 y 2.0
        val node1 = Node(id = 1, orderIndex = 1.0, title = "Primero")
        val node2 = Node(id = 2, orderIndex = 2.0, title = "Segundo")

        // Insertar entre ellos
        val newOrderIndex = (node1.orderIndex + node2.orderIndex) / 2.0

        assertEquals(1.5, newOrderIndex, 0.001)
        assertTrue(newOrderIndex > node1.orderIndex)
        assertTrue(newOrderIndex < node2.orderIndex)
    }

    @Test
    fun `fractional indexing works with multiple insertions`() {
        val orderIndices = mutableListOf(1.0, 2.0)

        // Insertar entre 1.0 y 2.0
        val new1 = (orderIndices[0] + orderIndices[1]) / 2.0 // 1.5
        orderIndices.add(1, new1)

        // Insertar entre 1.0 y 1.5
        val new2 = (orderIndices[0] + orderIndices[1]) / 2.0 // 1.25
        orderIndices.add(1, new2)

        // Insertar entre 1.25 y 1.5
        val new3 = (orderIndices[1] + orderIndices[2]) / 2.0 // 1.375

        assertEquals(1.375, new3, 0.001)

        // Verificar orden
        val sorted = listOf(new2, new3, new1, 2.0).sorted()
        assertEquals(listOf(1.25, 1.375, 1.5, 2.0), sorted)
    }

    @Test
    fun `node isRoot returns true for null parentId`() {
        val rootNode = Node(id = 1, parentId = null, title = "Root")
        val childNode = Node(id = 2, parentId = 1, title = "Child")

        assertTrue(rootNode.isRoot())
        assertFalse(childNode.isRoot())
    }

    @Test
    fun `withUpdatedTimestamp changes updatedAt`() {
        val original = Node(id = 1, title = "Test")
        val updated = original.withUpdatedTimestamp()

        assertTrue(updated.updatedAt.time >= original.updatedAt.time)
    }

    @Test
    fun `node hierarchy path building`() {
        // Crear jerarquía simulada
        val root = Node(id = 1, parentId = null, title = "Root")
        val child = Node(id = 2, parentId = 1, title = "Child")
        val grandchild = Node(id = 3, parentId = 2, title = "Grandchild")

        // Simular construcción de path
        val path = buildPath(grandchild, listOf(root, child, grandchild))

        assertEquals(2, path.size)
        assertEquals(root, path[0])
        assertEquals(child, path[1])
    }

    private fun buildPath(node: Node, allNodes: List<Node>): List<Node> {
        val path = mutableListOf<Node>()
        var currentParentId = node.parentId

        while (currentParentId != null) {
            val parent = allNodes.find { it.id == currentParentId }
            if (parent != null) {
                path.add(0, parent)
                currentParentId = parent.parentId
            } else {
                break
            }
        }
        return path
    }

    @Test
    fun `sibling order comparison`() {
        val siblings = listOf(
            Node(id = 1, orderIndex = 3.0, title = "Tercero"),
            Node(id = 2, orderIndex = 1.0, title = "Primero"),
            Node(id = 3, orderIndex = 2.5, title = "Segundo")
        )

        val sorted = siblings.sortedBy { it.orderIndex }

        assertEquals(2, sorted[0].id) // Primero
        assertEquals(3, sorted[1].id) // Segundo
        assertEquals(1, sorted[2].id) // Tercero
    }

    @Test
    fun `indent changes parent to previous sibling`() {
        // Simular indent: nodo 2 se convierte en hijo de nodo 1
        val node1 = Node(id = 1, parentId = null, orderIndex = 1.0, title = "Primero")
        val node2 = Node(id = 2, parentId = null, orderIndex = 2.0, title = "Segundo")

        // Después de indent
        val indentedNode2 = node2.copy(parentId = node1.id, orderIndex = 1.0)

        assertEquals(node1.id, indentedNode2.parentId)
        assertNull(node1.parentId)
    }

    @Test
    fun `outdent changes parent to grandparent`() {
        // Simular outdent: nodo hijo se convierte en hermano de su padre
        val grandparent = Node(id = 1, parentId = null, title = "Abuelo")
        val parent = Node(id = 2, parentId = 1, title = "Padre")
        val child = Node(id = 3, parentId = 2, title = "Hijo")

        // Después de outdent
        val outdentedChild = child.copy(parentId = grandparent.id, orderIndex = 2.0)

        assertEquals(grandparent.id, outdentedChild.parentId)
    }

    @Test
    fun `move up swaps order with previous sibling`() {
        val node1 = Node(id = 1, orderIndex = 1.0, title = "Primero")
        val node2 = Node(id = 2, orderIndex = 2.0, title = "Segundo")

        // Simular move up de node2
        val movedNode2 = node2.copy(orderIndex = node1.orderIndex)
        val movedNode1 = node1.copy(orderIndex = node2.orderIndex)

        assertEquals(1.0, movedNode2.orderIndex, 0.001)
        assertEquals(2.0, movedNode1.orderIndex, 0.001)
    }

    @Test
    fun `move down swaps order with next sibling`() {
        val node1 = Node(id = 1, orderIndex = 1.0, title = "Primero")
        val node2 = Node(id = 2, orderIndex = 2.0, title = "Segundo")

        // Simular move down de node1
        val movedNode1 = node1.copy(orderIndex = node2.orderIndex)
        val movedNode2 = node2.copy(orderIndex = node1.orderIndex)

        assertEquals(2.0, movedNode1.orderIndex, 0.001)
        assertEquals(1.0, movedNode2.orderIndex, 0.001)
    }
}
