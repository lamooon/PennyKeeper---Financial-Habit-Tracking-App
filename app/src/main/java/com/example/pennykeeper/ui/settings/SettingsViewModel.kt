package com.example.pennykeeper.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.data.repository.SettingsRepository
import com.example.pennykeeper.data.repository.ThemeRepository
import com.example.pennykeeper.utils.ExpensePrediction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import com.example.pennykeeper.utils.ai.OpenRouterInferenceSvc

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    //chatbot
    private val openRouterInferenceSvc = OpenRouterInferenceSvc(categoryRepository)

    data class ChatMessage(
        val content: String,
        val isUser: Boolean
    )

    // to implement darkmode
    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _monthlyExpenseTrend = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val monthlyExpenseTrend: StateFlow<List<Pair<String, Double>>> = _monthlyExpenseTrend.asStateFlow()

    //prediction service
    private val _predictedExpense = MutableStateFlow<Double>(0.0)
    val predictedExpense: StateFlow<Double> = _predictedExpense.asStateFlow()

    init {
        viewModelScope.launch {
            calculatePrediction()
        }
    }

    // Toggle the theme via ThemeRepository
    fun toggleTheme() {
        themeRepository.toggleDarkMode()
    }

    val budget: StateFlow<Double> = settingsRepository.getCurrentBudget()
        .map { it.dailyBudget }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _isBudgetSaved = MutableStateFlow(false)
    val isBudgetSaved: StateFlow<Boolean> = _isBudgetSaved.asStateFlow()

    fun saveBudget(budget: Double) {
        viewModelScope.launch {
            settingsRepository.saveBudget(budget)
            _isBudgetSaved.value = true
            kotlinx.coroutines.delay(2000)
            _isBudgetSaved.value = false
        }
    }

    private suspend fun calculatePrediction() {
        val expenses = expenseRepository.getAllExpenses()

        val monthlyExpenses = expenses
            .groupBy { expense ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = expense.date.time
                calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
            }
            .mapValues { it.value.sumOf { expense -> expense.amount } }
            .toList()
            .sortedBy { it.first }
            .mapIndexed { index, pair -> index to pair.second }

        val last6Months = monthlyExpenses.takeLast(6)
        _monthlyExpenseTrend.value = last6Months.map { (monthIndex, amount) ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, monthIndex - monthlyExpenses.size + 1)
            val monthYear = "${calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())} ${calendar.get(Calendar.YEAR)}"
            monthYear to amount
        }

        val prediction = ExpensePrediction().predictNextMonthExpense(monthlyExpenses)
        _predictedExpense.value = prediction
    }

    fun analyzeAllData() {
        viewModelScope.launch {
            _isAnalyzing.value = true

            try {
                val expenses = expenseRepository.getAllExpenses()
                val expenseContext = buildExpenseContext(expenses)

                _chatHistory.value = _chatHistory.value + ChatMessage(
                    "Analyze my spending patterns", true
                )

                val response = try {
                    openRouterInferenceSvc.getResponse("Analyze my spending patterns and provide financial advice.", expenseContext)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "API Error: ${e.message}")
                    generateBasicAnalysis(expenses)
                }

                _chatHistory.value = _chatHistory.value + ChatMessage(response, false)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    "Sorry, there was an error analyzing your expenses. Please try again.",
                    false
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            if (message.isBlank()) return@launch

            _isAnalyzing.value = true
            _chatHistory.value = _chatHistory.value + ChatMessage(message, true)

            try {
                val expenses = expenseRepository.getAllExpenses()
                if (expenses.isEmpty()) {
                    _chatHistory.value = _chatHistory.value + ChatMessage(
                        "I notice you don't have any expenses recorded yet. Please add some expenses first so I can help analyze them.",
                        false
                    )
                    return@launch
                }

                val expenseContext = buildExpenseContext(expenses)
                val response = try {
                    openRouterInferenceSvc.getResponse(message, expenseContext)
                } catch (e: Exception) {
                    Log.e("ChatBot", "Error calling API: ${e.message}")
                    generateBasicAnalysis(expenses)
                }

                _chatHistory.value = _chatHistory.value + ChatMessage(response, false)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    "Sorry, I couldn't process your request. Please try again.",
                    false
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private fun buildExpenseContext(expenses: List<ExpenseUiModel>): String {
        if (expenses.isEmpty()) return ""

        val total = expenses.sumOf { it.amount }
        return buildString {
            appendLine("Here are your recent expenses:")
            expenses.forEach { expense ->
                appendLine("- $${expense.amount} spent at ${expense.place} (${expense.categoryName}) on ${expense.date}")
            }
            appendLine("\nTotal spending: $${String.format("%.2f", total)}")
        }
    }

    private fun generateBasicAnalysis(expenses: List<ExpenseUiModel>): String {
        val total = expenses.sumOf { it.amount }
        val categorizedExpenses = expenses.groupBy { it.categoryName }
        val highestCategory = categorizedExpenses
            .maxByOrNull { (_, expenses) -> expenses.sumOf { it.amount } }

        return buildString {
            appendLine("📊 Basic Expense Analysis")
            appendLine("Total spending: $${String.format("%.2f", total)}")
            appendLine("Number of transactions: ${expenses.size}")

            if (highestCategory != null) {
                val categoryTotal = highestCategory.value.sumOf { it.amount }
                val percentage = (categoryTotal / total * 100).toInt()
                appendLine("\nHighest spending category: ${highestCategory.key}")
                appendLine("Amount: $${String.format("%.2f", categoryTotal)} ($percentage%)")
                appendLine("\nSuggestion: Consider setting a budget limit for ${highestCategory.key} to reduce expenses.")
            }
        }
    }

    //for debugging purpose
    private fun logResponse(response: String) {
        // Breaking down the response into chunks to avoid Log truncation
        response.chunked(1000).forEachIndexed { index, chunk ->
            Log.d("ChatResponse", "Part $index: $chunk")
        }
    }
}