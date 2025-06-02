package com.example.pennykeeper.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.pennykeeper.ui.theme.PREFS_NAME
import com.example.pennykeeper.ui.theme.PREF_DARK_MODE

class ThemeRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Flow to hold and emit the dark mode state
    private val _isDarkMode = MutableStateFlow(getThemePreference())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    // Reads the current theme preference from SharedPreferences
    private fun getThemePreference(): Boolean {
        return sharedPreferences.getBoolean(PREF_DARK_MODE, false) // Default to light mode
    }

    // Saves the dark mode state to SharedPreferences
    private fun saveThemePreference(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_DARK_MODE, isDarkMode).apply()
    }

    // Toggles the dark mode setting and saves it
    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        saveThemePreference(newMode)
    }
}