package com.example.pennykeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.data.repository.SettingsRepository
import com.example.pennykeeper.data.repository.ThemeRepository
import com.example.pennykeeper.ui.expense.EditExpenseViewModel
import com.example.pennykeeper.ui.home.HomeViewModel
import com.example.pennykeeper.ui.settings.CategoryViewModel
import com.example.pennykeeper.ui.settings.SettingsViewModel
import com.example.pennykeeper.ui.stats.StatisticsViewModel

class AppViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val themeRepository: ThemeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(expenseRepository, categoryRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> {
                StatisticsViewModel(expenseRepository) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsRepository, expenseRepository, categoryRepository, themeRepository) as T
            }

            modelClass.isAssignableFrom(EditExpenseViewModel::class.java) -> {
                EditExpenseViewModel(expenseRepository, categoryRepository) as T
            }

            modelClass.isAssignableFrom(CategoryViewModel::class.java) ->
                CategoryViewModel(categoryRepository) as T

            else -> throw IllegalArgumentException("ViewModel not found: ${modelClass.name}")
        }
    }
}