package com.example.pennykeeper.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pennykeeper.data.model.ExpenseUiModel
import com.example.pennykeeper.data.model.RecurringPeriod
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    var place by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurringPeriod by remember { mutableStateOf<RecurringPeriod?>(null) }
    var recurringPeriodExpanded by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val placeFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }

    val interactionSource = remember { MutableInteractionSource() }

    val calendar = Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                expanded = false
                recurringPeriodExpanded = false
                focusManager.clearFocus()
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add New Transaction") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Place") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(placeFocusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                        }
                    },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester),
                    singleLine = true
                )

            // Date
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, "Select date")
                    }
                }
            )

            // Category
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory.ifEmpty { "Select Category" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category.name
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Recurring Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring Expense")
                Switch(
                    checked = isRecurring,
                    onCheckedChange = {
                        isRecurring = it
                        if (!it) selectedRecurringPeriod = null
                    }
                )
            }

            // Recurring Period Dropdown
            if (isRecurring) {
                ExposedDropdownMenuBox(
                    expanded = recurringPeriodExpanded,
                    onExpandedChange = { recurringPeriodExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRecurringPeriod?.name ?: "Select Period",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurring Period") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = recurringPeriodExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = recurringPeriodExpanded,
                        onDismissRequest = { recurringPeriodExpanded = false }
                    ) {
                        RecurringPeriod.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.name) },
                                onClick = {
                                    selectedRecurringPeriod = period
                                    recurringPeriodExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
                Button(
                    onClick = {
                        if (place.isNotBlank() && amount.isNotBlank() &&
                            selectedDate.isNotBlank() && selectedCategory.isNotBlank()) {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            viewModel.addExpense(
                                ExpenseUiModel(
                                    amount = amount.toDouble(),
                                    place = place,
                                    categoryName = selectedCategory,
                                    date = dateFormat.parse(selectedDate)!!,
                                    isRecurring = isRecurring,
                                    recurringPeriod = selectedRecurringPeriod,
                                    nextDueDate = null
                                )
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = place.isNotBlank() &&
                            amount.isNotBlank() &&
                            selectedDate.isNotBlank() &&
                            selectedCategory.isNotBlank() &&
                            (!isRecurring || selectedRecurringPeriod != null)
                ) {
                    Text("Save Expense")
                }
            }
        }
    }
}