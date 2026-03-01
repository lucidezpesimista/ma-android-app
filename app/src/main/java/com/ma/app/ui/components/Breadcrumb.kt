package com.ma.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ma.app.data.model.Node

/**
 * Breadcrumb para navegación jerárquica.
 * Muestra: Root > Ancestro1 > Ancestro2 > Actual
 */
@Composable
fun Breadcrumb(
    ancestors: List<Node>,
    onNavigateToRoot: () -> Unit,
    onNavigateToAncestor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de volver
            if (ancestors.isNotEmpty()) {
                IconButton(
                    onClick = { 
                        ancestors.dropLast(1).lastOrNull()?.let { 
                            onNavigateToAncestor(it.id) 
                        } ?: onNavigateToRoot()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Breadcrumb scrollable
            LazyRow(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Root
                item {
                    BreadcrumbItem(
                        text = "間",
                        isClickable = ancestors.isNotEmpty(),
                        onClick = onNavigateToRoot
                    )
                }

                // Ancestros
                items(ancestors) { ancestor ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BreadcrumbItem(
                        text = ancestor.title.takeIf { it.isNotBlank() } ?: "(sin título)",
                        isClickable = true,
                        onClick = { onNavigateToAncestor(ancestor.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbItem(
    text: String,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    if (isClickable) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
