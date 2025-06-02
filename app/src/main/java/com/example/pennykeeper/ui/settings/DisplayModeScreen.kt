package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pennykeeper.ui.theme.saveThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayModeScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    // Observe the current dark mode state from the ViewModel
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = null // No scroll behavior, use null explicitly
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.titleLarge)

            // Switch for toggling dark mode
            Switch(
                checked = isDarkMode,
                onCheckedChange = { settingsViewModel.toggleTheme() },
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isDarkMode) "Dark Mode Enabled" else "Light Mode Enabled",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}