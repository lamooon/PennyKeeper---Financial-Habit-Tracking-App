package com.example.pennykeeper.data.repository

import com.example.pennykeeper.data.dao.CategoryDao
import com.example.pennykeeper.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acts as an API file for category file between server and DB
 */

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val defaultCategories: Flow<List<CategoryEntity>> = categoryDao.getDefaultCategories()

    suspend fun addCategory(name: String, isDefault: Boolean = false) {
        val category = CategoryEntity(
            name = name,
            isDefault = isDefault
        )
        categoryDao.insertIfNotExists(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getCategoryById(id: Int): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun getDefaultCategory(): CategoryEntity? {
        return categoryDao.getDefaultCategory()
    }

    suspend fun deleteNonDefaultCategories() {
        categoryDao.deleteNonDefaultCategories()
    }

    suspend fun ensureDefaultCategoriesExist() {
        val defaultCategories = listOf(
            "Groceries",
            "Transportation",
            "Entertainment",
            "Bills",
            "Shopping",
            "Other"
        )

        defaultCategories.forEach { categoryName ->
            addCategory(categoryName, isDefault = true)
        }
    }
}