package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * This model uses simple linear regression to predict monthly expenditure service
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val predictedExpense by settingsViewModel.predictedExpense.collectAsState()
    val monthlyTrend by settingsViewModel.monthlyExpenseTrend.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Prediction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Predicted Expense for Next Month",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$ %.2f".format(predictedExpense),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Last 6 Months Trend",
                style = MaterialTheme.typography.titleLarge
            )

            ExpenseChart(
                data = monthlyTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun ExpenseChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = "No historical data available",
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    val maxAmount = data.maxOf { it.second }
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val heightRatio = size.height / maxAmount

        // Draw bars
        data.forEachIndexed { index, (_, amount) ->
            val x = index * (barWidth * 2) + barWidth / 2
            val height = amount * heightRatio

            drawRect(
                color = primaryColor,
                topLeft = Offset(x, (size.height - height).toFloat()),
                size = Size(barWidth, height.toFloat())
            )
        }

        // Draw baseline
        drawLine(
            color = onSurfaceColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f
        )
    }

    // Labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        data.forEach { (month, _) ->
            Text(
                text = month,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}