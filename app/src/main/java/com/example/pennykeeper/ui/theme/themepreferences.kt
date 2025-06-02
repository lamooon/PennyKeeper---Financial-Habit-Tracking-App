package com.example.pennykeeper.ui.theme

import android.content.Context
import android.content.SharedPreferences

const val PREFS_NAME = "theme_prefs"
const val PREF_DARK_MODE = "is_dark_mode"

// Corrected the parameter type to Context
fun saveThemePreference(context: Context, isDarkMode: Boolean) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putBoolean(PREF_DARK_MODE, isDarkMode)
        apply()
    }
}

fun getThemePreference(context: Context): Boolean {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(PREF_DARK_MODE, false)
}

