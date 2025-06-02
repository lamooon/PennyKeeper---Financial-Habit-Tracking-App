package com.example.pennykeeper.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pennykeeper.data.model.RecurringPeriod
import android.app.DatePickerDialog
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    viewModel: EditExpenseViewModel,
    expenseId: Int,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId)
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val amountFocusRequester = remember { FocusRequester() }
    val placeFocusRequester = remember { FocusRequester() }

    val interactionSource = remember { MutableInteractionSource() }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth)
                viewModel.updateDate(calendar.time)
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                categoryExpanded = false
                periodExpanded = false
                focusManager.clearFocus()
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (expenseId == -1) "Add Expense" else "Edit Expense") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (expenseId != -1) {
                            IconButton(onClick = {
                                viewModel.deleteExpense(onNavigateBack)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            viewModel.updateAmount(it)
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.place,
                    onValueChange = { viewModel.updatePlace(it) },
                    label = { Text("Place") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(placeFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateFormatter.format(viewModel.date),
                    onValueChange = { },
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    enabled = false,
                    //because color didn't match with other fields for some reason
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = viewModel.categoryName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.updateCategory(category)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring Expense")
                    Switch(
                        checked = viewModel.isRecurring,
                        onCheckedChange = { viewModel.updateRecurring(it) }
                    )
                }

                if (viewModel.isRecurring) {
                    ExposedDropdownMenuBox(
                        expanded = periodExpanded,
                        onExpandedChange = { periodExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = viewModel.recurringPeriod?.name ?: "Select Period",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Recurring Period") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false }
                        ) {
                            RecurringPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.name) },
                                    onClick = {
                                        viewModel.updateRecurringPeriod(period)
                                        periodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.saveExpense(onNavigateBack)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.isValid()
                ) {
                    Text(if (expenseId == -1) "Add" else "Save")
                }
            }
        }
    }
}