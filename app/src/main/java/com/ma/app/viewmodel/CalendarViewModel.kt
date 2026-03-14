package com.ma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ma.app.data.model.Node
import com.ma.app.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class CalendarUiState(
    val selectedDate: Date = Date(),
    val displayMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val displayYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val nodesForSelectedDay: List<Node> = emptyList(),
    val datesWithNodes: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

class CalendarViewModel(private val repository: NodeRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(startOfToday())
    private val _displayMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _displayYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _datesWithNodes = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _displayMonth,
        _displayYear,
        _datesWithNodes
    ) { selectedDate, month, year, datesWithNodes ->
        CalendarUiState(
            selectedDate = selectedDate,
            displayMonth = month,
            displayYear = year,
            datesWithNodes = datesWithNodes
        )
    }.flatMapLatest { state ->
        // Cargar nodos del día seleccionado
        repository.getTasksForDay(state.selectedDate).map { nodes ->
            state.copy(nodesForSelectedDay = nodes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    init {
        loadDatesWithNodes()
    }

    fun selectDate(date: Date) {
        _selectedDate.value = startOfDay(date)
    }

    fun previousMonth() {
        val month = _displayMonth.value
        val year = _displayYear.value
        if (month == 0) {
            _displayMonth.value = 11
            _displayYear.value = year - 1
        } else {
            _displayMonth.value = month - 1
        }
        loadDatesWithNodes()
    }

    fun nextMonth() {
        val month = _displayMonth.value
        val year = _displayYear.value
        if (month == 11) {
            _displayMonth.value = 0
            _displayYear.value = year + 1
        } else {
            _displayMonth.value = month + 1
        }
        loadDatesWithNodes()
    }

    private fun loadDatesWithNodes() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(_displayYear.value, _displayMonth.value, 1)
            val start = cal.time
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = cal.time

            val dates = repository.getDatesWithNodes(start, end)
            _datesWithNodes.value = dates.toSet()
        }
    }

    private fun startOfToday(): Date = startOfDay(Date())

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    companion object {
        fun provideFactory(repository: NodeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(repository) as T
                }
            }
        }
    }
}
