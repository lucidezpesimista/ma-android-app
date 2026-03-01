package com.ma.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.model.NodeWithPath
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.SearchViewModel

/**
 * Pantalla de búsqueda con contexto jerárquico.
 * 
 * DECISIÓN: Los resultados muestran el path completo (Ancestros > Item)
 * para dar contexto de dónde se encuentra cada resultado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: NodeRepository,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long, Long?) -> Unit // nodeId, parentId
) {
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de búsqueda
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = { /* La búsqueda es automática con debounce */ },
                onClear = viewModel::clearSearch
            )

            // Tag seleccionado (si hay)
            if (uiState.selectedTag != null) {
                SelectedTagChip(
                    tag = uiState.selectedTag,
                    onClear = viewModel::clearTagFilter
                )
            }

            // Resultados
            when {
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.results.isEmpty() && uiState.hasSearched -> {
                    EmptySearchResults()
                }
                uiState.results.isNotEmpty() -> {
                    SearchResultsList(
                        results = uiState.results,
                        onResultClick = { result ->
                            val parentId = result.node.parentId
                            onNavigateToNode(result.node.id, parentId)
                        },
                        onHashtagClick = viewModel::searchByTag
                    )
                }
                else -> {
                    // Estado inicial - mostrar ayuda
                    SearchHelp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Buscar ítems...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SelectedTagChip(
    tag: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtrando por:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        InputChip(
            selected = true,
            onClick = onClear,
            label = { Text("#$tag") },
            trailingIcon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar filtro",
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

@Composable
private fun SearchResultsList(
    results: List<NodeWithPath>,
    onResultClick: (NodeWithPath) -> Unit,
    onHashtagClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { result ->
            SearchResultItem(
                result = result,
                onClick = { onResultClick(result) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: NodeWithPath,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Path (contexto jerárquico)
            if (result.path.isNotEmpty()) {
                Text(
                    text = result.path.joinToString(" > ") { 
                        it.title.takeIf { t -> t.isNotBlank() } ?: "(sin título)" 
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Título del nodo
            Text(
                text = result.node.title.takeIf { it.isNotBlank() } ?: "(sin título)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preview de la nota si existe
            result.node.note?.let { note ->
                if (note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.take(100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResults() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se encontraron resultados",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchHelp() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Busca en tus notas",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Escribe para buscar en títulos y contenido. " +
                       "Los resultados muestran el contexto jerárquico.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "También puedes buscar por hashtags como #trabajo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
