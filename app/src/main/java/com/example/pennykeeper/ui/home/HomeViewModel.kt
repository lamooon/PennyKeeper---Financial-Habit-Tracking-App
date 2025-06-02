package com.example.pennykeeper.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.model.RecurringPeriod
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HomeViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Fetch daily budget directly as a flow
    val dailyBudgetFlow = settingsRepository.getDailyBudgetFlow()

    val expenses = expenseRepository.expenses
        .map { it.sortedByDescending { expense -> expense.date } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    private val _uiState = MutableStateFlow(HomeUiState())

    val categories = categoryRepository.categories





    fun addExpense(expenseUiModel: ExpenseUiModel) {
        viewModelScope.launch {
            try {
                expenseRepository.addExpense(expenseUiModel.copy(
                    nextDueDate = calculateNextDueDate(expenseUiModel.date, expenseUiModel.recurringPeriod)
                ))
                _uiState.update { it.copy(isExpenseAdded = true) }
            } catch (e: IllegalArgumentException) {
            }
        }
    }



    private fun calculateNextDueDate(currentDate: Date, recurringPeriod: RecurringPeriod?): Date? {
        if (recurringPeriod == null) return null

        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        when (recurringPeriod) {
            RecurringPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }

        return calendar.time
    }


    fun deleteAllExpenses() {
        viewModelScope.launch {
            expenseRepository.deleteAllExpenses()
        }
    }

}

data class HomeUiState(
    val isExpenseAdded: Boolean = false
)