package com.example.pennykeeper.data.repository

import android.content.Context
import com.example.pennykeeper.data.dao.SettingsDao
import com.example.pennykeeper.data.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SettingsRepository(private val settingsDao: SettingsDao) {

    // Get current budget from the database
    fun getCurrentBudget(): Flow<Budget> {
        return settingsDao.getBudget()
            .map { budget ->
                budget ?: Budget(dailyBudget = 0.0) // Provide default Budget if null
            }
    }


    // Save new budget to the database
    suspend fun saveBudget(budget: Double) {
        settingsDao.insert(Budget(dailyBudget = budget))
    }

    fun getDailyBudgetFlow(): Flow<Double> {
        return getCurrentBudget()
            .map { budget ->
                budget.dailyBudget
            }
    }


}
