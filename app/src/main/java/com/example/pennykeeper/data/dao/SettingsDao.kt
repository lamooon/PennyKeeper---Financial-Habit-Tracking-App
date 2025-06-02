package com.example.pennykeeper.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pennykeeper.data.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * This file allows users to set their daily budgets
 */
@Dao
interface SettingsDao {


    @Query("SELECT * FROM settings_table ORDER BY id DESC LIMIT 1")
    fun getBudget(): Flow<Budget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

}
