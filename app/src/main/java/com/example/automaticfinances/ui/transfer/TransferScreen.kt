package com.example.automaticfinances.ui.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.ui.components.common.FormErrorCard
import com.example.automaticfinances.ui.components.common.SaveButton
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.theme.FinanceShapes
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: TransferViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

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
                        "Transferencia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.section),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item {
                Text(
                    text = "Mueve dinero entre tus cuentas. No cuenta como ingreso ni gasto: solo " +
                        "ajusta el saldo de cada cuenta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Amount
            item {
                SectionCard {
                    Text("Monto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = viewModel::setAmount,
                        label = { Text("Cantidad en COP") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.amountError != null,
                        supportingText = state.amountError?.let { { Text(it) } },
                        singleLine = true,
                        shape = FinanceShapes.textField,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Origin
            item {
                AccountPickerCard(
                    title = "Desde",
                    accounts = state.accounts,
                    selectedId = state.originAccountId,
                    onSelected = viewModel::setOrigin,
                    numberFormat = numberFormat,
                )
            }

            // Swap button
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FilledTonalIconButton(onClick = viewModel::swap) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Invertir cuentas")
                    }
                }
            }

            // Destination
            item {
                AccountPickerCard(
                    title = "Hacia",
                    accounts = state.accounts,
                    selectedId = state.destAccountId,
                    onSelected = viewModel::setDest,
                    numberFormat = numberFormat,
                )
            }

            // Balance preview
            if (state.canSave) {
                item {
                    TransferPreviewCard(
                        origin = state.originAccount,
                        dest = state.destAccount,
                        amountCents = state.amountCents,
                        numberFormat = numberFormat,
                    )
                }
            }

            // Note
            item {
                SectionCard {
                    Text("Nota (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        label = { Text("Ej: Retiro cajero, ahorro...") },
                        maxLines = 3,
                        shape = FinanceShapes.textField,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.error?.let { error ->
                item { FormErrorCard(message = error) }
            }

            item {
                SaveButton(
                    text = "Transferir",
                    onClick = { viewModel.save() },
                    enabled = state.canSave,
                    loading = state.isLoading,
                    loadingText = "Transfiriendo…",
                    modifier = Modifier.padding(vertical = Spacing.sm),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerCard(
    title: String,
    accounts: List<Account>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    numberFormat: NumberFormat,
) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(Spacing.md))
        accounts.forEach { account ->
            val selected = account.id == selectedId
            val icon = if (account.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments
            Surface(
                onClick = { onSelected(account.id) },
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            "Saldo: ${numberFormat.format(account.balanceCents / 100.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = "Seleccionada", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferPreviewCard(
    origin: Account?,
    dest: Account?,
    amountCents: Long,
    numberFormat: NumberFormat,
) {
    if (origin == null || dest == null) return
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text("Después de la transferencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BalanceRow(
                label = origin.name,
                before = origin.balanceCents,
                after = origin.balanceCents - amountCents,
                numberFormat = numberFormat,
            )
            BalanceRow(
                label = dest.name,
                before = dest.balanceCents,
                after = dest.balanceCents + amountCents,
                numberFormat = numberFormat,
            )
        }
    }
}

@Composable
private fun BalanceRow(label: String, before: Long, after: Long, numberFormat: NumberFormat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            numberFormat.format(before / 100.0),
            style = FinanceTypography.moneySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowRightAlt,
            contentDescription = null,
            modifier = Modifier.size(Sizes.iconSm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            numberFormat.format(after / 100.0),
            style = FinanceTypography.moneySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (after >= 0) FinanceTheme.colors.profit else MaterialTheme.colorScheme.error,
        )
    }
}
