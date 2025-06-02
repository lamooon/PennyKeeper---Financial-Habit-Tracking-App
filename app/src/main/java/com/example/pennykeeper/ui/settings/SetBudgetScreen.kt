package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val budgetState by settingsViewModel.budget.collectAsState()
    var newBudgetInput by remember { mutableStateOf(budgetState.toString()) }
    val isBudgetSaved by settingsViewModel.isBudgetSaved.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Current Budget: ${formatCurrency(budgetState)}",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = newBudgetInput,
                onValueChange = { newBudgetInput = it },
                label = { Text("Set New Budget") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = newBudgetInput.isNotEmpty() && newBudgetInput.toDoubleOrNull() == null
            )

            Button(
                onClick = {
                    val budget = newBudgetInput.toDoubleOrNull()
                    if (budget != null) {
                        settingsViewModel.saveBudget(budget)
                    }
                },
                enabled = newBudgetInput.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Budget")
            }

            if (isBudgetSaved) {
                Text(
                    text = "Budget saved successfully!",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}