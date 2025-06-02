package com.example.pennykeeper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pennykeeper.data.model.CategoryEntity
import com.example.pennykeeper.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository
        .categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String, isDefault: Boolean = false) {
        viewModelScope.launch {
            categoryRepository.addCategory(name, isDefault)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}