package com.example.pennykeeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This is category table.
 * Relationship: [CategoryEntity] <-- [Expense] (one to many)
 */

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isDefault: Boolean = false
)