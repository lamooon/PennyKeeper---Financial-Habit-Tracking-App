package com.example.pennykeeper.utils

/**
 * ExpensePrediction implements Simple Exponential Smoothing (SES) for monthly expense forecasting.
 *
 * Algorithm:
 * - Uses exponential smoothing with α (alpha) = 0.7
 * - Formula: Ft+1 = α * Yt + (1-α) * Ft
 * - Gives more weight to recent observations (70% recent, 30% historical) suitable for short term
 * with limited data
 *
 */

class ExpensePrediction {
    fun predictNextMonthExpense(expenses: List<Pair<Int, Double>>): Double {
        if (expenses.size < 2) return 0.0

        val values = expenses.map { it.second }

        val alpha = 0.7

        // Initialize with first value
        var forecast = values[0]

        // Calculate smoothed values
        for (i in 1 until values.size) {
            forecast = alpha * values[i] + (1 - alpha) * forecast
        }

        // return prediction
        return forecast
    }
}