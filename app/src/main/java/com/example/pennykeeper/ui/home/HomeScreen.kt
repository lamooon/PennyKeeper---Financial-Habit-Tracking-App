package com.example.pennykeeper.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pennykeeper.data.model.ExpenseUiModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import android.app.Activity
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import com.example.pennykeeper.util.SpeechRecognitionHelper


@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToAdd: () -> Unit,
) {
    val expenses by homeViewModel.expenses.collectAsState()
    val dailyLimit by homeViewModel.dailyBudgetFlow.collectAsState(initial = 0.0)

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Expenses") },
            text = { Text("Are you sure you want to delete all expenses? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.deleteAllExpenses()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    //speech recognizer
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            Log.d("SpeechDebug", "Raw spoken text: $spokenText")

            spokenText?.let { text ->
                // Parse amount
                val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
                val amountMatch = amountRegex.find(text)
                val amount = amountMatch?.groupValues?.get(1)
                Log.d("SpeechDebug", "Extracted amount: $amount")

                // Parse date
                val calendar = Calendar.getInstance()

                val date = when {
                    // Handle "on the Xth/Xst/Xnd/Xrd"
                    text.contains(Regex("""on the (\d+)(st|nd|rd|th)""", RegexOption.IGNORE_CASE)) -> {
                        val dayMatch = Regex("""on the (\d+)""").find(text)
                        val day = dayMatch?.groupValues?.get(1)?.toIntOrNull()
                        if (day != null && day in 1..31) {
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                        }
                        calendar.time
                    }

                    // Handle "last [day]"
                    text.contains(Regex("""last (monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE)) -> {
                        val dayMatch = Regex("""last (monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE).find(text)
                        val dayOfWeek = when (dayMatch?.groupValues?.get(1)?.lowercase()) {
                            "sunday" -> Calendar.SUNDAY
                            "monday" -> Calendar.MONDAY
                            "tuesday" -> Calendar.TUESDAY
                            "wednesday" -> Calendar.WEDNESDAY
                            "thursday" -> Calendar.THURSDAY
                            "friday" -> Calendar.FRIDAY
                            "saturday" -> Calendar.SATURDAY
                            else -> null
                        }

                        if (dayOfWeek != null) {
                            // Move to previous week
                            calendar.add(Calendar.WEEK_OF_YEAR, -1)
                            // Set to the specified day
                            while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                                calendar.add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        calendar.time
                    }

                    text.contains("yesterday", ignoreCase = true) -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        calendar.time
                    }
                    text.contains("tomorrow", ignoreCase = true) -> {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        calendar.time
                    }
                    else -> Date()
                }

                // Clean up the text to get place
                var place = text
                    // Remove common action words
                    .replace(Regex("""^(add|spent|used|paid|spence|spend|use)\s+""", RegexOption.IGNORE_CASE), "")
                    // Remove amount with possible dollar sign
                    .replace(Regex("""\$?\s*\d+(?:\.\d{1,2})?"""), "")
                    // Remove common prepositions
                    .replace(Regex("""\s+(on|at|in|over|for)\s+""", RegexOption.IGNORE_CASE), " ")
                    // Remove date references
                    .replace(Regex("""\s+(yesterday|today|tomorrow)""", RegexOption.IGNORE_CASE), "")
                    // Remove "on the Xth" date format
                    .replace(Regex("""\s+on the \d+(st|nd|rd|th)""", RegexOption.IGNORE_CASE), "")
                    // Remove "last [day]" format
                    .replace(Regex("""\s+last (monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE), "")
                    .trim()


                if (amount != null) {
                    homeViewModel.addExpense(
                        ExpenseUiModel(
                            amount = amount.toDouble(),
                            place = place,
                            categoryName = "Other",
                            date = date,
                            isRecurring = false,
                            recurringPeriod = null,
                            nextDueDate = null
                        )
                    )
                }
            }
        }
    }


    // Compute the spent amount for today
    val spentAmount = remember(expenses) {
        val today = Calendar.getInstance()
        expenses.filter { expense ->
            val expenseDate = Calendar.getInstance().apply { time = expense.date }
            expenseDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    expenseDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Determine message based on spending ratio
    val message = when {
        dailyLimit == 0.0 -> ""
        spentAmount / dailyLimit > 1.0 -> "🚨 You've exceeded your budget! 🐷💸"
        spentAmount / dailyLimit > 0.8 -> "⚠️ You're at ${"%.1f".format((spentAmount / dailyLimit) * 100)}%! 🐷"
        spentAmount / dailyLimit > 0.5 -> "😊 Keep an eye on spending: ${"%.1f".format((spentAmount / dailyLimit) * 100)}%."
        spentAmount / dailyLimit > 0.3 -> "👍 Doing well! ${"%.1f".format((spentAmount / dailyLimit) * 100)}% used. 🌟"
        spentAmount / dailyLimit > 0.1 -> "✨ Great start! ${"%.1f".format((spentAmount / dailyLimit) * 100)}% spent. 🌈"
        else -> ""
    }

    // Show the appropriate snackbar message
    LaunchedEffect(spentAmount, dailyLimit) {
        if (message.isNotEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showDeleteConfirmation = true },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete All")
                }
                FloatingActionButton(
                    onClick = {
                        SpeechRecognitionHelper.startSpeechRecognition(speechLauncher)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = "Voice Input"
                    )
                }
                FloatingActionButton(
                    onClick = onNavigateToAdd
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.33f)
                            .fillMaxWidth()
                    ) {
                        HabitTrackerSection(dailyLimit = dailyLimit, spentAmount = spentAmount)
                    }

                    // SnackbarHost with centered text
                    SnackbarHost(
                        hostState = snackbarHostState,
                        snackbar = { snackbarData ->
                            Snackbar(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    text = snackbarData.visuals.message,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(0.67f)
                            .fillMaxWidth()
                    ) {
                        if (expenses.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Press + button to add expenses.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                items(expenses) { expense ->
                                    ExpenseCard(
                                        expense = expense,
                                        onClick = { onNavigateToEdit(expense.id) }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@SuppressLint("DefaultLocale")
@Composable
private fun HabitTrackerSection(
    dailyLimit: Double,
    spentAmount: Double
) {
    val remainingBudget = dailyLimit - spentAmount
    val progress = if (dailyLimit > 0) {
        (spentAmount / dailyLimit).coerceIn(0.0, 1.0)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Daily Limit Text
        Text(
            text = "Daily Limit: $${String.format("%.2f", dailyLimit)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Progress Bar
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        )

        // Spent and Remaining Text
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: $${String.format("%.2f", spentAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Remaining: $${String.format("%.2f", remainingBudget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ExpenseCard(
    expense: ExpenseUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Place and Category
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.place,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expense.isRecurring) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recurring",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Right side: Amount and Date
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = SimpleDateFormat("MMM d", Locale.getDefault())
                        .format(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }


}