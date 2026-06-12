package com.example.automaticfinances.ui.transaction

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
fun AddTransactionScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AddTransactionViewModel = hiltViewModel()
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
                        "Nuevo gasto",
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            // Live amount hero — mirrors what will be stored as the user types.
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
                            color = FinanceTheme.colors.loss
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Monto (COP)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { { Text(it) } },
                    shape = FinanceShapes.textField,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen),
                    leadingIcon = { Text("$") }
                )
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Descripción") },
                    placeholder = { Text("Almuerzo, transporte, etc.") },
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
                        onValueChange = viewModel::updateDate,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        value = state.selectedTime,
                        onValueChange = viewModel::updateTime,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Selector de cuenta (permite registrar gastos en efectivo o asociados a un banco)
            if (state.accounts.isNotEmpty()) {
                item {
                    AccountSelectorCard(
                        accounts = state.accounts,
                        selectedAccountId = state.selectedAccountId,
                        onAccountSelected = viewModel::selectAccount,
                        numberFormat = numberFormat,
                        modifier = Modifier.padding(horizontal = Spacing.screen)
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    SectionHeader(title = "Categoría")
                    CategoryChipsGrid(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = viewModel::selectCategory
                    )
                }
            }

            state.errorMessage?.let { error ->
                item {
                    FormErrorCard(
                        message = error,
                        modifier = Modifier.padding(horizontal = Spacing.screen)
                    )
                }
            }

            item {
                SaveButton(
                    text = "Guardar gasto",
                    onClick = { viewModel.saveTransaction() },
                    loading = state.isLoading,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                )
            }
        }
    }
}
