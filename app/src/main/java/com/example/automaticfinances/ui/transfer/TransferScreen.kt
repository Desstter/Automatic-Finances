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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.ui.theme.FinanceTheme
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
                title = { Text("Transferencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = state.canSave) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text("Guardar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Monto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.amount,
                            onValueChange = viewModel::setAmount,
                            label = { Text("Cantidad en COP") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = state.amountError != null,
                            supportingText = state.amountError?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nota (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = viewModel::setNote,
                            label = { Text("Ej: Retiro cajero, ahorro...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = state.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
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
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            accounts.forEach { account ->
                val selected = account.id == selectedId
                val icon = if (account.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments
                Card(
                    onClick = { onSelected(account.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = if (selected) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors()
                    },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun TransferPreviewCard(
    origin: Account?,
    dest: Account?,
    amountCents: Long,
    numberFormat: NumberFormat,
) {
    if (origin == null || dest == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Después de la transferencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            numberFormat.format(before / 100.0),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowRightAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            numberFormat.format(after / 100.0),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (after >= 0) FinanceTheme.colors.profit else MaterialTheme.colorScheme.error,
        )
    }
}
