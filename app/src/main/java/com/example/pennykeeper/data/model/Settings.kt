package com.example.pennykeeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This file is the table for the daily budget
 */

@Entity(tableName = "settings_table")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyBudget: Double
)
