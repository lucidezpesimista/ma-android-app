package com.ma.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.repository.NodeRepository
import com.ma.app.ui.components.Breadcrumb
import com.ma.app.ui.components.OutlineItem
import com.ma.app.viewmodel.OutlineUiState
import com.ma.app.viewmodel.OutlineViewModel

/**
 * Pantalla principal del outliner.
 * 
 * DECISIÓN DE UX:
 * - FAB flotante para agregar nuevo ítem al final
 * - Breadcrumb en la parte superior para navegación
 * - Lista con items editables inline
 * - Pull-to-refresh no es necesario (datos locales)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineScreen(
    repository: NodeRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToNodeDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val viewModel: OutlineViewModel = viewModel(
        factory = OutlineViewModel.provideFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("間") },
                navigationIcon = {
                    if (uiState.currentNodeId != null) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNodeAtEnd() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar ítem")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Breadcrumb
            if (uiState.breadcrumb.isNotEmpty() || uiState.currentNodeId != null) {
                Breadcrumb(
                    ancestors = uiState.breadcrumb,
                    onNavigateToRoot = { viewModel.navigateToRoot() },
                    onNavigateToAncestor = { viewModel.navigateToAncestor(it) }
                )
            }

            // Lista de nodos
            if (uiState.nodes.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    onAddItem = { viewModel.createNodeAtEnd() }
                )
            } else {
                NodesList(
                    nodes = uiState.nodes,
                    editingNodeId = uiState.editingNodeId,
                    repository = repository,
                    onStartEditing = { viewModel.startEditing(it) },
                    onStopEditing = { viewModel.stopEditing() },
                    onTitleChange = { id, title -> viewModel.updateNodeTitle(id, title) },
                    onFocusNode = { viewModel.focusOnNode(it) },
                    onIndent = { viewModel.indent(it) },
                    onOutdent = { viewModel.outdent(it) },
                    onMoveUp = { viewModel.moveUp(it) },
                    onMoveDown = { viewModel.moveDown(it) },
                    onDelete = { viewModel.deleteNode(it) },
                    onHashtagClick = { tag ->
                        // Navegar a búsqueda con el tag
                        onNavigateToSearch()
                    },
                    onInternalLinkClick = { link ->
                        // Buscar nodo por título y navegar
                        // TODO: Implementar navegación a nodo por link
                    }
                )
            }
        }
    }
}

@Composable
private fun NodesList(
    nodes: List<com.ma.app.data.model.Node>,
    editingNodeId: Long?,
    repository: NodeRepository,
    onStartEditing: (Long) -> Unit,
    onStopEditing: () -> Unit,
    onTitleChange: (Long, String) -> Unit,
    onFocusNode: (Long) -> Unit,
    onIndent: (com.ma.app.data.model.Node) -> Unit,
    onOutdent: (com.ma.app.data.model.Node) -> Unit,
    onMoveUp: (com.ma.app.data.model.Node) -> Unit,
    onMoveDown: (com.ma.app.data.model.Node) -> Unit,
    onDelete: (Long) -> Unit,
    onHashtagClick: (String) -> Unit,
    onInternalLinkClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = nodes,
            key = { it.id }
        ) { node ->
            var childrenCount by remember { mutableStateOf(0) }

            LaunchedEffect(node.id) {
                childrenCount = repository.getChildrenCount(node.id)
            }

            OutlineItem(
                node = node,
                isEditing = editingNodeId == node.id,
                hasChildren = childrenCount > 0,
                childrenCount = childrenCount,
                onStartEditing = { onStartEditing(node.id) },
                onStopEditing = onStopEditing,
                onTitleChange = { onTitleChange(node.id, it) },
                onFocusNode = { onFocusNode(node.id) },
                onIndent = { onIndent(node) },
                onOutdent = { onOutdent(node) },
                onMoveUp = { onMoveUp(node) },
                onMoveDown = { onMoveDown(node) },
                onDelete = { onDelete(node.id) },
                onToggleCollapsed = { /* TODO */ },
                onHashtagClick = onHashtagClick,
                onInternalLinkClick = onInternalLinkClick
            )
        }
    }
}

@Composable
private fun EmptyState(
    onAddItem: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.NoteAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sin ítems",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Toca + para agregar tu primer ítem",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar ítem")
            }
        }
    }
}
