package com.ma.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ma.app.data.model.Node
import com.ma.app.utils.TextParser
import kotlinx.coroutines.delay

/**
 * Item individual del outliner.
 * 
 * DECISIÓN DE UX - Interacciones:
 * - Tap en el texto: inicia edición inline
 * - Tap en el chevron: focus en esa rama
 * - Swipe derecha: indent
 * - Swipe izquierda: outdent
 * - Long press: menú contextual
 * - Drag handle (los 3 puntos): reordenar
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlineItem(
    node: Node,
    isEditing: Boolean,
    hasChildren: Boolean,
    childrenCount: Int,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    onTitleChange: (String) -> Unit,
    onFocusNode: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onInternalLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var isSwiping by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isSwiping = true },
                    onDragEnd = { isSwiping = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (kotlin.math.abs(dragAmount) > 50) {
                            if (dragAmount > 0) {
                                onIndent()
                            } else {
                                onOutdent()
                            }
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEditing) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chevron para expandir/colapsar y focus
            IconButton(
                onClick = onFocusNode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (hasChildren) {
                        Icons.Default.KeyboardArrowRight
                    } else {
                        Icons.Outlined.Circle
                    },
                    contentDescription = if (hasChildren) "Enfocar" else "Sin hijos",
                    tint = if (hasChildren) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    }
                )
            }

            // Contenido principal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (isEditing) {
                    InlineEditor(
                        text = node.title,
                        onTextChange = onTitleChange,
                        onDone = onStopEditing
                    )
                } else {
                    ClickableNodeText(
                        text = node.title,
                        onClick = onStartEditing,
                        onHashtagClick = onHashtagClick,
                        onInternalLinkClick = onInternalLinkClick
                    )
                }
            }

            // Contador de hijos
            if (hasChildren && childrenCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = childrenCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Menú contextual
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Indentar →") },
                        onClick = {
                            onIndent()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FormatIndentIncrease, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("← Desindentar") },
                        onClick = {
                            onOutdent()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FormatIndentDecrease, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mover arriba") },
                        onClick = {
                            onMoveUp()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mover abajo") },
                        onClick = {
                            onMoveDown()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Editor inline para edición rápida de nodos.
 */
@Composable
private fun InlineEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }

    LaunchedEffect(Unit) {
        delay(50) // Pequeño delay para asegurar que el foco se aplique
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { 
            textFieldValue = it
            onTextChange(it.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                innerTextField()
            }
        }
    )
}

/**
 * Texto clickeable con soporte para hashtags y links.
 */
@Composable
private fun ClickableNodeText(
    text: String,
    onClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onInternalLinkClick: (String) -> Unit
) {
    val parsedText = remember(text) { TextParser.parse(text) }

    ClickableText(
        text = parsedText.annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            // Verificar si clickeamos en un hashtag o link
            parsedText.annotatedString.getStringAnnotations("hashtag", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick(annotation.item)
                    return@clickableText
                }

            parsedText.annotatedString.getStringAnnotations("internal_link", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onInternalLinkClick(annotation.item)
                    return@clickableText
                }

            // Si no, iniciar edición
            onClick()
        }
    )
}
