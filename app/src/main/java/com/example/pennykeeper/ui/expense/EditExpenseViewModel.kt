package com.example.pennykeeper.ui.expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pennykeeper.data.model.CategoryEntity
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.model.RecurringPeriod
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class EditExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    var expense by mutableStateOf<ExpenseUiModel?>(null)
        private set

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    var amount by mutableStateOf("")
        private set
    var place by mutableStateOf("")
        private set
    var categoryName by mutableStateOf("")
        private set
    var date by mutableStateOf(Date())
        private set
    var isRecurring by mutableStateOf(false)
        private set
    var recurringPeriod by mutableStateOf<RecurringPeriod?>(null)
        private set

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategoriesExist()
            categoryRepository.categories.collect {
                _categories.value = it
            }
        }
    }

    fun loadExpense(id: Int) {
        if (id != -1) {
            viewModelScope.launch {
                expense = expenseRepository.getExpenseById(id)
                expense?.let { exp ->
                    amount = exp.amount.toString()
                    place = exp.place
                    categoryName = exp.categoryName
                    date = exp.date
                    isRecurring = exp.isRecurring
                    recurringPeriod = exp.recurringPeriod
                }
            }
        } else {
            // Set default category for new expense
            viewModelScope.launch {
                val defaultCategory = categoryRepository.getDefaultCategory()
                categoryName = defaultCategory?.name ?: ""
            }
        }
    }

    fun updateAmount(newAmount: String) {
        amount = newAmount
    }

    fun updatePlace(newPlace: String) {
        place = newPlace
    }

    fun updateCategory(category: CategoryEntity) {
        categoryName = category.name
    }

    fun updateDate(newDate: Date) {
        date = newDate
    }

    fun updateRecurring(recurring: Boolean) {
        isRecurring = recurring
        if (!recurring) {
            recurringPeriod = null
        }
    }

    fun updateRecurringPeriod(period: RecurringPeriod) {
        recurringPeriod = period
    }

    fun saveExpense(onComplete: () -> Unit) {
        viewModelScope.launch {
            val amountDouble = amount.toDoubleOrNull() ?: return@launch
            if (categoryName.isEmpty()) return@launch

            val newExpense = expense?.copy(
                amount = amountDouble,
                place = place,
                categoryName = categoryName,
                date = date,
                isRecurring = isRecurring,
                recurringPeriod = recurringPeriod,
                nextDueDate = if (isRecurring) calculateNextDueDate(date, recurringPeriod!!) else null
            ) ?: ExpenseUiModel(
                amount = amountDouble,
                place = place,
                categoryName = categoryName,
                date = date,
                isRecurring = isRecurring,
                recurringPeriod = recurringPeriod,
                nextDueDate = if (isRecurring) calculateNextDueDate(date, recurringPeriod!!) else null
            )

            if (expense == null) {
                expenseRepository.addExpense(newExpense)
            } else {
                expenseRepository.updateExpense(newExpense)
            }
            onComplete()
        }
    }

    private fun calculateNextDueDate(currentDate: Date, period: RecurringPeriod): Date {
        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        when (period) {
            RecurringPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }

        return calendar.time
    }

    fun deleteExpense(onComplete: () -> Unit) {
        viewModelScope.launch {
            expense?.let {
                expenseRepository.deleteExpense(it)
                onComplete()
            }
        }
    }

    fun isValid(): Boolean {
        return amount.isNotEmpty() &&
                place.isNotEmpty() &&
                categoryName.isNotEmpty() &&
                (!isRecurring || recurringPeriod != null)
    }
}