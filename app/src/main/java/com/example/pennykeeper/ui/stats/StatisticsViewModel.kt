package com.example.pennykeeper.ui.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(private val repository: ExpenseRepository) : ViewModel() {
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _currentDate = MutableStateFlow(Calendar.getInstance())

    private val _currentMonth = MutableStateFlow(_currentDate.value.get(Calendar.MONTH) + 1)
    val currentMonth = _currentMonth.asStateFlow()

    private val _currentWeek = MutableStateFlow(getWeekOfMonth(_currentDate.value))

    private val _currentYear = MutableStateFlow(_currentDate.value.get(Calendar.YEAR))
    val currentYear = _currentYear.asStateFlow()

    private val _weeksInCurrentMonth = MutableStateFlow(getWeeksInMonth(_currentDate.value))

    private val _categoryExpenses = MutableStateFlow<List<CategoryExpense>>(emptyList())
    val categoryExpenses = _categoryExpenses.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount = _totalAmount.asStateFlow()

    data class CategoryExpense(
        val categoryName: String,
        val amount: Double,
        val color: Color,
        val percentage: Float
    )

    enum class TimePeriod {
        MONTH,
        YEAR
    }

    private val availableColors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFF44336), // Red
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF607D8B)  // Gray
    )

    private var categoryColorMap = mutableMapOf<String, Color>()

    init {
        viewModelScope.launch {
            combine(
                _selectedPeriod,
                _currentDate
            ) { period, date ->
                Pair(period, date)
            }.flatMapLatest { (period, date) ->
                repository.getExpensesByPeriod(
                    when (period) {
                        TimePeriod.MONTH -> ExpenseRepository.TimePeriod.MONTH
                        TimePeriod.YEAR -> ExpenseRepository.TimePeriod.YEAR
                    },
                    date.time
                )
            }.collect { expenses ->
                updateStatistics(expenses)
            }
        }
    }

    private fun getWeekOfMonth(calendar: Calendar): Int {
        val clone = calendar.clone() as Calendar
        clone.setMinimalDaysInFirstWeek(1)
        return clone.get(Calendar.WEEK_OF_MONTH)
    }

    private fun getWeeksInMonth(calendar: Calendar): Int {
        val clone = calendar.clone() as Calendar
        clone.setMinimalDaysInFirstWeek(1)

        // Set to first day of month
        clone.set(Calendar.DAY_OF_MONTH, 1)

        // Set to last day of month
        clone.set(Calendar.DAY_OF_MONTH, clone.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastWeek = clone.get(Calendar.WEEK_OF_MONTH)

        return lastWeek
    }

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        updateCurrentDate(Calendar.getInstance())
    }

    fun setMonth(month: Int) {
        val newDate = _currentDate.value.clone() as Calendar
        newDate.set(Calendar.MONTH, month - 1) // Months are 0-based in Calendar
        newDate.set(Calendar.DAY_OF_MONTH, 1) // Set to first day of month to avoid date overflow
        updateCurrentDate(newDate)
    }

    private fun updateCurrentDate(calendar: Calendar) {
        _currentDate.value = calendar
        _currentMonth.value = calendar.get(Calendar.MONTH) + 1
        _currentWeek.value = getWeekOfMonth(calendar)
        _currentYear.value = calendar.get(Calendar.YEAR)
        _weeksInCurrentMonth.value = getWeeksInMonth(calendar)

        // Trigger expense update
        viewModelScope.launch {
            repository.getExpensesByPeriod(
                when (_selectedPeriod.value) {
                    TimePeriod.MONTH -> ExpenseRepository.TimePeriod.MONTH
                    TimePeriod.YEAR -> ExpenseRepository.TimePeriod.YEAR
                },
                calendar.time
            ).collect { expenses ->
                updateStatistics(expenses)
            }
        }
    }


    fun setYear(year: Int) {
        val newDate = _currentDate.value.clone() as Calendar
        newDate.set(Calendar.YEAR, year)
        updateCurrentDate(newDate)
    }




    private fun updateStatistics(expenses: List<ExpenseUiModel>) {
        val total = expenses.sumOf { it.amount }
        _totalAmount.value = total

        // Group expenses by category name
        val categoryAmounts = expenses
            .groupBy { it.categoryName }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        // Assign colors to categories if not already assigned
        categoryAmounts.keys.forEach { categoryName ->
            if (!categoryColorMap.containsKey(categoryName)) {
                val nextColor = availableColors[categoryColorMap.size % availableColors.size]
                categoryColorMap[categoryName] = nextColor
            }
        }

        _categoryExpenses.value = categoryAmounts
            .filter { it.value > 0 }
            .map { (categoryName, amount) ->
                CategoryExpense(
                    categoryName = categoryName,
                    amount = amount,
                    color = categoryColorMap[categoryName] ?: Color.Gray,
                    percentage = if (total > 0) (amount / total).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }
    }

}