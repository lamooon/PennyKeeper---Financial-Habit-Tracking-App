package com.example.pennykeeper.data.dao

import androidx.room.*
import com.example.pennykeeper.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * This is an interface so user can add, update, delete the categories as they wish
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCategory(): CategoryEntity?

    @Insert
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name)")
    suspend fun categoryExists(name: String): Boolean

    @Transaction
    suspend fun insertIfNotExists(category: CategoryEntity) {
        val exists = categoryExists(category.name)
        if (!exists) {
            insertCategory(category)
        }
    }

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    fun getDefaultCategories(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteNonDefaultCategories()
}