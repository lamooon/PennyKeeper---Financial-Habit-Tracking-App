package com.example.pennykeeper

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pennykeeper.data.dao.CategoryDao
import com.example.pennykeeper.data.dao.ExpenseDao
import com.example.pennykeeper.data.database.ExpenseDatabase
import com.example.pennykeeper.data.model.CategoryEntity
import com.example.pennykeeper.data.model.Expense
import com.example.pennykeeper.data.model.RecurringPeriod
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ExpenseDatabaseTest {
    private lateinit var db: ExpenseDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            ExpenseDatabase::class.java
        ).build()
        expenseDao = db.expenseDao()
        categoryDao = db.categoryDao()
        expenseRepository = ExpenseRepository(expenseDao, categoryDao)
        categoryRepository = CategoryRepository(categoryDao)

        // Prepopulate categories
        runBlocking {
            populateCategories()
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun populateCategories() {
        val defaultCategories = listOf(
            "Groceries", "Subscriptions", "Taxes",
            "Entertainment", "Utilities", "Other"
        )
        defaultCategories.forEach { name ->
            categoryDao.insertCategory(CategoryEntity(name = name, isDefault = true))
        }
    }

    private fun createTestExpense(
        amount: Double,
        place: String,
        categoryId: Int,
        date: Date,
        isRecurring: Boolean = false,
        recurringPeriod: RecurringPeriod? = null
    ): Expense {
        return Expense(
            amount = amount,
            place = place,
            categoryId = categoryId,
            date = date,
            isRecurring = isRecurring,
            recurringPeriod = recurringPeriod,
            nextDueDate = if (isRecurring) calculateNextDueDate(date, recurringPeriod!!) else null
        )
    }

    private fun calculateNextDueDate(date: Date, period: RecurringPeriod): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        when (period) {
            RecurringPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.time
    }

    private fun createDateForTest(year: Int, month: Int, day: Int): Date {
        return Calendar.getInstance().apply {
            set(year, month - 1, day)
        }.time
    }

    @Test
    fun testPrepopulatedData() = runBlocking {
        // Test data spanning 2023-2024
        val testExpenses = listOf(
            // 2023 expenses
            createTestExpense(50.0, "Walmart", 1, createDateForTest(2023, 12, 15)),
            createTestExpense(100.0, "Netflix", 2, createDateForTest(2023, 12, 1), true, RecurringPeriod.MONTHLY),
            createTestExpense(1000.0, "Property Tax", 3, createDateForTest(2023, 11, 15), true, RecurringPeriod.YEARLY),

            // 2024 expenses
            createTestExpense(75.0, "Target", 1, createDateForTest(2024, 1, 5)),
            createTestExpense(30.0, "Movie Theater", 4, createDateForTest(2024, 2, 14)),
            createTestExpense(120.0, "Electric Bill", 5, createDateForTest(2024, 3, 1), true, RecurringPeriod.MONTHLY)
        )

        // Insert test expenses
        testExpenses.forEach { expense ->
            expenseDao.insertExpense(expense)
        }

        // Verify data insertion
        val allExpenses = expenseDao.getAllExpenses().first()
        assert(allExpenses.size == testExpenses.size)

        // Test recurring expenses
        val recurringExpenses = expenseDao.getRecurringExpenses().first()
        assert(recurringExpenses.count { it.isRecurring } == 3)

        // Test expenses by category
        val groceryExpenses = expenseDao.getExpensesByCategory(1).first()
        assert(groceryExpenses.count() == 2) // Walmart and Target
    }

    @Test
    fun testCRUDOperations() = runBlocking {
        // Test Create
        val expense = createTestExpense(
            amount = 50.0,
            place = "Test Place",
            categoryId = 1,
            date = Date()
        )
        expenseDao.insertExpense(expense)

        // Test Read
        val allExpenses = expenseDao.getAllExpenses().first()
        assert(allExpenses.isNotEmpty())

        // Get the inserted expense from the list
        val insertedExpense = allExpenses.first()

        // Test Update
        val updatedExpense = insertedExpense.copy(amount = 75.0)
        expenseDao.updateExpense(updatedExpense)

        // Verify update
        val expensesAfterUpdate = expenseDao.getAllExpenses().first()
        assert(expensesAfterUpdate.first().amount == 75.0) {
            "Expected amount: 75.0, Actual amount: ${expensesAfterUpdate.first().amount}"
        }

        // Test Delete
        expenseDao.deleteExpense(updatedExpense)
        val expensesAfterDelete = expenseDao.getAllExpenses().first()
        assert(expensesAfterDelete.isEmpty()) {
            "Expected empty list, but got ${expensesAfterDelete.size} expenses"
        }
    }
}