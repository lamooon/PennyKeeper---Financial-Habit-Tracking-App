package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pennykeeper.data.model.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: CategoryViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val categories by viewModel.categories.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Category")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { editingCategory = category },
                    onDelete = { viewModel.deleteCategory(category) },
                    enabled = !category.isDefault
                )
            }
        }

        if (showAddDialog) {
            CategoryDialog(
                category = null,
                onDismiss = { showAddDialog = false },
                onSave = { name ->
                    viewModel.addCategory(name)
                    showAddDialog = false
                }
            )
        }

        editingCategory?.let { category ->
            CategoryDialog(
                category = category,
                onDismiss = { editingCategory = null },
                onSave = { name ->
                    viewModel.updateCategory(category.copy(name = name))
                    editingCategory = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.name)
            if (enabled) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}