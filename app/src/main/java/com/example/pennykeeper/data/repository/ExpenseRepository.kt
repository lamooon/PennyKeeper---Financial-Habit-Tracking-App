package com.example.pennykeeper.data.repository

import com.example.pennykeeper.data.dao.ExpenseDao
import com.example.pennykeeper.data.dao.CategoryDao
import com.example.pennykeeper.data.model.Expense
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.model.RecurringPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    val expenses: Flow<List<ExpenseUiModel>> = combine(
        expenseDao.getAllExpenses(),
        categoryDao.getAllCategories()
    ) { expenses, categories ->
        expenses.map { expense ->
            val category = categories.find { it.id == expense.categoryId }
            ExpenseUiModel(
                id = expense.id,
                amount = expense.amount,
                place = expense.place,
                categoryName = category?.name ?: "Unknown",
                date = expense.date,
                isRecurring = expense.isRecurring,
                recurringPeriod = expense.recurringPeriod,
                nextDueDate = expense.nextDueDate
            )
        }
    }


    fun getExpensesByPeriod(period: TimePeriod, date: Date = Date()): Flow<List<ExpenseUiModel>> {
        return expenses.map { expenseList ->
            val calendar = Calendar.getInstance()
            calendar.time = date

            when (period) {
                TimePeriod.WEEK -> {
                    val startCalendar = calendar.clone() as Calendar
                    startCalendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    val endCalendar = startCalendar.clone() as Calendar
                    endCalendar.add(Calendar.DAY_OF_WEEK, 6)

                    expenseList.filter {
                        val expenseDate = Calendar.getInstance().apply { time = it.date }
                        expenseDate.after(startCalendar) && expenseDate.before(endCalendar)
                    }
                }
                TimePeriod.MONTH -> {
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    expenseList.mapNotNull { expense ->
                        val expenseDate = Calendar.getInstance().apply { time = expense.date }
                        val expenseMonth = expenseDate.get(Calendar.MONTH)
                        val expenseYear = expenseDate.get(Calendar.YEAR)

                        when {
                            // Regular non-recurring expenses
                            !expense.isRecurring && expenseMonth == currentMonth && expenseYear == currentYear -> {
                                expense
                            }
                            // Monthly recurring expenses
                            expense.isRecurring && expense.recurringPeriod == RecurringPeriod.MONTHLY &&
                                    (expenseYear < currentYear || (expenseYear == currentYear && expenseMonth <= currentMonth)) -> {
                                expense
                            }
                            // Yearly recurring expenses
                            expense.isRecurring && expense.recurringPeriod == RecurringPeriod.YEARLY &&
                                    isExpenseActiveInYear(expenseDate, calendar) -> {
                                expense.copy(amount = expense.amount / 12)
                            }
                            else -> null
                        }
                    }
                }
                TimePeriod.YEAR -> {
                    val currentYear = calendar.get(Calendar.YEAR)

                    expenseList.filter { expense ->
                        val expenseDate = Calendar.getInstance().apply { time = expense.date }
                        val expenseYear = expenseDate.get(Calendar.YEAR)

                        when {
                            !expense.isRecurring -> expenseYear == currentYear
                            expense.recurringPeriod == RecurringPeriod.MONTHLY -> {
                                expenseYear <= currentYear
                            }
                            expense.recurringPeriod == RecurringPeriod.YEARLY -> {
                                isExpenseActiveInYear(expenseDate, calendar)
                            }
                            else -> false
                        }
                    }
                }
            }
        }
    }

    private fun isExpenseActiveInYear(expenseDate: Calendar, currentDate: Calendar): Boolean {
        val expenseYear = expenseDate.get(Calendar.YEAR)
        val currentYear = currentDate.get(Calendar.YEAR)

        return expenseYear <= currentYear
    }

    suspend fun addExpense(expense: ExpenseUiModel) {
        val categoryId = categoryDao.getCategoryByName(expense.categoryName)?.id
            ?: throw IllegalArgumentException("Category not found")

        val dbExpense = Expense(
            id = expense.id,
            amount = expense.amount,
            place = expense.place,
            categoryId = categoryId,
            date = expense.date,
            isRecurring = expense.isRecurring,
            recurringPeriod = expense.recurringPeriod,
            nextDueDate = expense.nextDueDate
        )
        expenseDao.insertExpense(dbExpense)
    }

    suspend fun updateExpense(expense: ExpenseUiModel) {
        val categoryId = categoryDao.getCategoryByName(expense.categoryName)?.id
            ?: throw IllegalArgumentException("Category not found")

        val dbExpense = Expense(
            id = expense.id,
            amount = expense.amount,
            place = expense.place,
            categoryId = categoryId,
            date = expense.date,
            isRecurring = expense.isRecurring,
            recurringPeriod = expense.recurringPeriod,
            nextDueDate = expense.nextDueDate
        )
        expenseDao.updateExpense(dbExpense)
    }

    suspend fun deleteExpense(expense: ExpenseUiModel) {
        val categoryId = categoryDao.getCategoryByName(expense.categoryName)?.id
            ?: throw IllegalArgumentException("Category not found")

        val dbExpense = Expense(
            id = expense.id,
            amount = expense.amount,
            place = expense.place,
            categoryId = categoryId,
            date = expense.date,
            isRecurring = expense.isRecurring,
            recurringPeriod = expense.recurringPeriod,
            nextDueDate = expense.nextDueDate
        )
        expenseDao.deleteExpense(dbExpense)
    }

    suspend fun getExpenseById(id: Int): ExpenseUiModel? {
        val expense = expenseDao.getExpenseById(id) ?: return null
        val category = categoryDao.getCategoryById(expense.categoryId)

        return ExpenseUiModel(
            id = expense.id,
            amount = expense.amount,
            place = expense.place,
            categoryName = category?.name ?: "Unknown",
            date = expense.date,
            isRecurring = expense.isRecurring,
            recurringPeriod = expense.recurringPeriod,
            nextDueDate = expense.nextDueDate
        )
    }

    suspend fun deleteAllExpenses() {
        expenseDao.deleteAll()
    }

    //for prediction service
    suspend fun getAllExpenses(): List<ExpenseUiModel> {
        return combine(
            expenseDao.getAllExpenses(),
            categoryDao.getAllCategories()
        ) { expenses, categories ->
            expenses.map { expense ->
                val category = categories.find { it.id == expense.categoryId }
                ExpenseUiModel(
                    id = expense.id,
                    amount = expense.amount,
                    place = expense.place,
                    categoryName = category?.name ?: "Unknown",
                    date = expense.date,
                    isRecurring = expense.isRecurring,
                    recurringPeriod = expense.recurringPeriod,
                    nextDueDate = expense.nextDueDate
                )
            }
        }.first()
    }

    enum class TimePeriod {
        WEEK, MONTH, YEAR
    }
}