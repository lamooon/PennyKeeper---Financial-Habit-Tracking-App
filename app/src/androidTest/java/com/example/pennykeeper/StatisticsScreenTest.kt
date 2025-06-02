package com.example.pennykeeper

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pennykeeper.data.dao.CategoryDao
import com.example.pennykeeper.data.dao.ExpenseDao
import com.example.pennykeeper.data.database.ExpenseDatabase
import com.example.pennykeeper.data.model.CategoryEntity
import com.example.pennykeeper.data.model.Expense
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.ui.stats.StatisticsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    private lateinit var db: ExpenseDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var viewModel: StatisticsViewModel

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

        viewModel = StatisticsViewModel(expenseRepository)

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
            CategoryEntity(name = "Groceries", isDefault = true),
            CategoryEntity(name = "Transport", isDefault = true),
            CategoryEntity(name = "Entertainment", isDefault = true)
        )
        defaultCategories.forEach { category ->
            categoryDao.insertCategory(category)
        }
    }

    private suspend fun populateExpenses() {
        val testExpenses = listOf(
            Expense(amount = 200.0, place = "Supermarket", categoryId = 1, date = Date()),
            Expense(amount = 300.0, place = "Train Ticket", categoryId = 2, date = Date()),
            Expense(amount = 500.0, place = "Cinema", categoryId = 3, date = Date())
        )
        testExpenses.forEach { expense ->
            expenseDao.insertExpense(expense)
        }
    }

    @Test
    fun testStatisticsCalculation() = runBlocking {
        // Prepopulate expenses
        populateExpenses()

        // Validate database state
        val expenses = expenseDao.getAllExpenses().first()
        assertEquals(3, expenses.size) // Ensure test data is correctly inserted

        // Trigger the ViewModel to fetch and process data
        viewModel.setPeriod(StatisticsViewModel.TimePeriod.MONTH)

        // Wait for the flow to emit values
        val totalAmount = viewModel.totalAmount.first()
        val categoryExpenses = viewModel.categoryExpenses.first()

        // Verify categoryExpenses is not empty
        assertTrue(categoryExpenses.isNotEmpty())

        // Verify total amount
        assertEquals(1000.0, totalAmount, 0.01) // Add delta for floating-point comparison

        // Verify category data
        val groceries = categoryExpenses.find { it.categoryName == "Groceries" }
        val transport = categoryExpenses.find { it.categoryName == "Transport" }
        val entertainment = categoryExpenses.find { it.categoryName == "Entertainment" }

        assertNotNull("Groceries category not found", groceries)
        assertNotNull("Transport category not found", transport)
        assertNotNull("Entertainment category not found", entertainment)

        assertEquals(0.2f, groceries!!.percentage, 0.01f)
        assertEquals(0.3f, transport!!.percentage, 0.01f)
        assertEquals(0.5f, entertainment!!.percentage, 0.01f)

        // Verify colors are assigned
        assertTrue(groceries.color != Color.Gray)
        assertTrue(transport.color != Color.Gray)
        assertTrue(entertainment.color != Color.Gray)
    }

    @Test
    fun testEmptyExpenses() = runBlocking {
        // Ensure the ViewModel processes the empty database
        viewModel.setPeriod(StatisticsViewModel.TimePeriod.MONTH)

        // Wait for the flow to emit values
        val totalAmount = viewModel.totalAmount.first()
        val categoryExpenses = viewModel.categoryExpenses.first()

        // Verify empty stats
        assertTrue(categoryExpenses.isEmpty())
        assertEquals(0.0, totalAmount, 0.01) // Add delta for floating-point comparison
    }
}

