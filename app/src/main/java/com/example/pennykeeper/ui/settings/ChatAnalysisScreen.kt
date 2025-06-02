package com.example.pennykeeper.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAnalysisScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    val chatHistory by settingsViewModel.chatHistory.collectAsState()
    val isLoading by settingsViewModel.isAnalyzing.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { focusManager.clearFocus() },
        topBar = {
            TopAppBar(
                title = { Text("Financial Assistant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about your finances...") },
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            settingsViewModel.sendMessage(userInput)
                            userInput = ""
                            focusManager.clearFocus()
                        },
                        enabled = userInput.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                FilledTonalButton(
                    onClick = { settingsViewModel.analyzeAllData() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Analyze All My Data")
                }
            }

            items(chatHistory) { message ->
                SelectionContainer {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (message.isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}