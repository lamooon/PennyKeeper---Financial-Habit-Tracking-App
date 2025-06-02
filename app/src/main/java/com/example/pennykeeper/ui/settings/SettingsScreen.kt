package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.*

fun formatCurrency(amount: Double): String {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US) // Always use US locale
    return currencyFormatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateToBudget: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToDisplay: () -> Unit,
    onNavigateToChatAnalysis: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilledTonalButton(
                    onClick = onNavigateToBudget,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Budget")
                }
            }

            item {
                FilledTonalButton(
                    onClick = onNavigateToCategories,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Categories")
                }
            }

            item {
                FilledTonalButton(
                    onClick = onNavigateToPrediction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Expense Prediction")
                }
            }

            item {
                FilledTonalButton(
                    onClick = onNavigateToDisplay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Display Mode")
                }
            }

            item {
                FilledTonalButton(
                    onClick = onNavigateToChatAnalysis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Financial Assistant")
                }
            }
        }
    }
}