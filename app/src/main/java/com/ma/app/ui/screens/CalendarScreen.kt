package com.ma.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ma.app.data.model.Node
import com.ma.app.data.model.Priority
import com.ma.app.data.repository.NodeRepository
import com.ma.app.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: NodeRepository,
    onNavigateToNode: (Long) -> Unit = {}
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cabecera del calendario (mes + año + navegación)
            CalendarHeader(
                month = uiState.displayMonth,
                year = uiState.displayYear,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth
            )

            // Cuadrícula del calendario
            CalendarGrid(
                month = uiState.displayMonth,
                year = uiState.displayYear,
                selectedDate = uiState.selectedDate,
                datesWithNodes = uiState.datesWithNodes,
                onSelectDate = viewModel::selectDate
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Fecha seleccionada
            val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
            Text(
                text = dateFormat.format(uiState.selectedDate).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Lista de tareas/eventos del día
            if (uiState.nodesForSelectedDay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin tareas para este día",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.nodesForSelectedDay, key = { it.id }) { node ->
                        CalendarTaskItem(
                            node = node,
                            onClick = { onNavigateToNode(node.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Mes anterior")
        }

        Text(
            text = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Mes siguiente")
        }
    }
}

@Composable
private fun CalendarGrid(
    month: Int,
    year: Int,
    selectedDate: Date,
    datesWithNodes: Set<String>,
    onSelectDate: (Date) -> Unit
) {
    val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
    val today = Calendar.getInstance()

    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val selectedCal = Calendar.getInstance()
    selectedCal.time = selectedDate

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Nombres de días
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Días del mes
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1

                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayCal = Calendar.getInstance()
                        dayCal.set(year, month, day, 0, 0, 0)
                        dayCal.set(Calendar.MILLISECOND, 0)
                        val dayDate = dayCal.time

                        val isSelected = selectedCal.get(Calendar.YEAR) == year &&
                                selectedCal.get(Calendar.MONTH) == month &&
                                selectedCal.get(Calendar.DAY_OF_MONTH) == day

                        val isToday = today.get(Calendar.YEAR) == year &&
                                today.get(Calendar.MONTH) == month &&
                                today.get(Calendar.DAY_OF_MONTH) == day

                        // Verificar si hay nodos en este día
                        val dateKey = String.format("%04d-%02d-%02d", year, month + 1, day)
                        val hasNodes = datesWithNodes.contains(dateKey)

                        CalendarDay(
                            day = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasNodes = hasNodes,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectDate(dayDate) }
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasNodes: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasNodes) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

@Composable
private fun CalendarTaskItem(
    node: Node,
    onClick: () -> Unit
) {
    val priority = node.priorityEnum()
    val priorityColor = when (priority) {
        Priority.HIGH -> Color(0xFFD32F2F)
        Priority.MEDIUM -> Color(0xFFFF8F00)
        Priority.LOW -> Color(0xFF388E3C)
        Priority.NONE -> MaterialTheme.colorScheme.outline
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "ES"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador de prioridad
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.title.ifBlank { "(sin título)" },
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (node.isCompleted) TextDecoration.LineThrough else null,
                    color = if (node.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                node.dueDate?.let { date ->
                    Text(
                        text = timeFormat.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (node.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completada",
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
