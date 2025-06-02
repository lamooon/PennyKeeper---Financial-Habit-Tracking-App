package com.example.pennykeeper.data.model

import java.util.Date

/**
 *
 * This is different from Expense.kt,
 * This is UI representation that does not need
 * Room DB assistance - using this so it's easier to reference in homescreen and statsscreen.
 *
 */

data class ExpenseUiModel(
    val id: Int = 0,
    val amount: Double,
    val place: String,
    val categoryName: String, // Actual category name not ID
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val nextDueDate: Date? = null
)

