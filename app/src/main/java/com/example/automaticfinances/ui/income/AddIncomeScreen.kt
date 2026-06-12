package com.example.automaticfinances.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.ui.components.AccountSelectorCard
import com.example.automaticfinances.ui.components.AccountBalancePreview
import com.example.automaticfinances.ui.components.common.CategoryChipsGrid
import com.example.automaticfinances.ui.components.common.DatePickerField
import com.example.automaticfinances.ui.components.common.FormErrorCard
import com.example.automaticfinances.ui.components.common.SaveButton
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.components.common.TimePickerField
import com.example.automaticfinances.ui.theme.FinanceShapes
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AddIncomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val numberFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.resetSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nuevo ingreso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            // Live amount hero — same layout as the gasto form, tinted profit green.
            item {
                val formattedAmount = remember(state.amount) {
                    val raw = state.amount.replace(",", ".").toDoubleOrNull()
                    if (raw != null && raw > 0) numberFormat.format(raw) else "$0"
                }
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = formattedAmount,
                            style = FinanceTypography.moneyLarge,
                            color = FinanceTheme.colors.profit
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = { Text("Monto (COP)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { { Text(it) } },
                    shape = FinanceShapes.textField,
                    leadingIcon = { Text("$") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen)
                )
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    label = { Text("Descripción") },
                    placeholder = { Text("Salario, freelance, ventas…") },
                    isError = state.descriptionError != null,
                    supportingText = state.descriptionError?.let { { Text(it) } },
                    shape = FinanceShapes.textField,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen),
                    maxLines = 2
                )
            }

            // Fecha y hora — abren date/time pickers de Material 3.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    DatePickerField(
                        value = state.selectedDate,
                        onValueChange = viewModel::setDate,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        value = state.selectedTime,
                        onValueChange = viewModel::setTime,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                AccountSelectorCard(
                    accounts = state.availableAccounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = viewModel::setAccount,
                    numberFormat = numberFormat,
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
            }

            // Preview del saldo resultante mientras se escribe el monto.
            if (state.selectedAccount != null && state.incomeAmountCents > 0) {
                item {
                    Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                        AccountBalancePreview(
                            account = state.selectedAccount,
                            incomeAmount = state.incomeAmountCents,
                            numberFormat = numberFormat
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    SectionHeader(title = "Categoría")
                    CategoryChipsGrid(
                        categories = state.incomeCategories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = viewModel::setCategory
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    label = { Text("Notas (opcional)") },
                    shape = FinanceShapes.textField,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen)
                )
            }

            state.error?.let { error ->
                item {
                    FormErrorCard(
                        message = error,
                        modifier = Modifier.padding(horizontal = Spacing.screen)
                    )
                }
            }

            item {
                SaveButton(
                    text = "Guardar ingreso",
                    onClick = { viewModel.saveIncome() },
                    enabled = state.canSave,
                    loading = state.isLoading,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                )
            }
        }
    }
}
