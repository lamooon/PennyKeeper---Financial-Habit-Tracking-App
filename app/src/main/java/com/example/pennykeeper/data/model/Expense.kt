package com.example.pennykeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * This is an expense file where user stores their transactions on Room DB
 */

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("categoryId")] // foreign key
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val place: String,
    val categoryId: Int, //foreign key
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val nextDueDate: Date? = null
)

enum class RecurringPeriod {
    MONTHLY,
    YEARLY
}