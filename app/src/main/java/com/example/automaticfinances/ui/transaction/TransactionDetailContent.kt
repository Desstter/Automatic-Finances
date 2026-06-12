package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.ui.components.common.CategoryChipsGrid
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.theme.FinanceShapes
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat

// Stateless body of the transaction-detail screen. Everything here receives its data via [state]
// and reports user intent through hoisted callbacks — no ViewModel is passed in — so the pieces are
// independently previewable and only recompose on the data they actually read.

@Composable
internal fun TransactionDetailContent(
    state: TransactionDetailState,
    numberFormat: NumberFormat,
    onUndo: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onOriginSelected: (Long) -> Unit,
    onDestSelected: (Long) -> Unit,
    categoryFor: (Long) -> Category?,
    modifier: Modifier = Modifier
) {
    val transaction = state.transaction!!

    // Show deleted state with undo option
    if (state.isDeleted) {
        DeletedTransactionContent(
            onUndo = onUndo,
            transactionAmount = numberFormat.format(transaction.amountCents / 100.0),
            transactionDescription = transaction.description,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Receipt-style hero header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            // Sign + color follow the movement type, matching the convention used in every
            // list: green +ingreso, red −gasto, neutral transfer (a transfer moves money,
            // it doesn't gain or lose it).
            Text(
                text = buildString {
                    when {
                        state.isTransfer -> {}
                        transaction.isIncome -> append("+ ")
                        else -> append("− ")
                    }
                    append(numberFormat.format(transaction.amountCents / 100.0))
                },
                style = FinanceTypography.moneyLarge,
                color = when {
                    state.isTransfer -> MaterialTheme.colorScheme.secondary
                    transaction.isIncome -> FinanceTheme.colors.profit
                    else -> FinanceTheme.colors.loss
                }
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "${transaction.date} · ${transaction.time}",
                style = FinanceTypography.dateTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section)
        ) {
            TransactionDetailCards(
                state = state,
                numberFormat = numberFormat,
                onDescriptionChange = onDescriptionChange,
                onNotesChange = onNotesChange,
                onCategorySelected = onCategorySelected,
                onAccountSelected = onAccountSelected,
                onOriginSelected = onOriginSelected,
                onDestSelected = onDestSelected,
                categoryFor = categoryFor
            )
        }
    }
}

@Composable
private fun DeletedTransactionContent(
    onUndo: () -> Unit,
    transactionAmount: String,
    transactionDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Transacción eliminada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$transactionAmount - $transactionDescription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Todos los cálculos se han actualizado automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUndo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deshacer eliminación")
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailCards(
    state: TransactionDetailState,
    numberFormat: NumberFormat,
    onDescriptionChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onOriginSelected: (Long) -> Unit,
    onDestSelected: (Long) -> Unit,
    categoryFor: (Long) -> Category?
) {
    val transaction = state.transaction!!

    // Card con información básica. El monto y la fecha ya viven en el hero de arriba,
    // así que aquí solo va lo que el hero no muestra: tipo de movimiento y tarjeta/cuenta.
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = FinanceShapes.statusIndicator,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = transaction.type,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (transaction.srcLast4 != null || transaction.dstLast4 != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconSm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when {
                        transaction.srcLast4 != null && transaction.dstLast4 != null ->
                            "De *${transaction.srcLast4} a *${transaction.dstLast4}"
                        transaction.srcLast4 != null -> "Tarjeta *${transaction.srcLast4}"
                        else -> "A cuenta *${transaction.dstLast4}"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    // Cuenta (editable) — para una transferencia se editan origen y destino; para una transacción
    // normal, la única cuenta a la que pertenece el movimiento.
    AccountCard(
        state = state,
        numberFormat = numberFormat,
        onAccountSelected = onAccountSelected,
        onOriginSelected = onOriginSelected,
        onDestSelected = onDestSelected
    )

    // Descripción. Para transferencias se deriva automáticamente de las cuentas, así que se muestra
    // de solo lectura para no contradecir el origen/destino seleccionados.
    SectionCard {
        Text(
            text = "Descripción",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (state.isEditMode && !state.isTransfer) {
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = FinanceShapes.textField,
                singleLine = true
            )
        } else {
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    // Categoría (editable) — una transferencia no tiene categoría (no es ingreso ni gasto).
    if (!state.isTransfer) {
        SectionCard {
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (state.isEditMode) {
                CategorySelector(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = onCategorySelected
                )
            } else {
                val category = state.selectedCategoryId?.let { categoryFor(it) }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category?.icon ?: "•",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = category?.name ?: "Sin categoría",
                        style = MaterialTheme.typography.bodyLarge,
                        color = category?.color?.let {
                            Color(android.graphics.Color.parseColor(it))
                        } ?: MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Notas (editable)
    SectionCard {
        Text(
            text = "Notas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (state.isEditMode) {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Agregar notas...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = FinanceShapes.textField,
                minLines = 3,
                maxLines = 5
            )
        } else {
            if (state.notes.isNotBlank()) {
                Text(
                    text = state.notes,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Sin notas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    state: TransactionDetailState,
    numberFormat: NumberFormat,
    onAccountSelected: (Long) -> Unit,
    onOriginSelected: (Long) -> Unit,
    onDestSelected: (Long) -> Unit
) {
    val accounts = state.accounts
    fun nameOf(id: Long?): String = accounts.find { it.id == id }?.name ?: "—"

    SectionCard {
        Text(
            text = if (state.isTransfer) "Cuentas" else "Cuenta",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (state.isTransfer) {
            if (state.isEditMode) {
                Text("Desde", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(Spacing.xs))
                AccountChipsRow(accounts, state.originAccountId, onOriginSelected)
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("Hacia", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(Spacing.xs))
                AccountChipsRow(accounts, state.destAccountId, onDestSelected)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = nameOf(state.originAccountId), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = "hacia",
                        modifier = Modifier.size(Sizes.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(text = nameOf(state.destAccountId), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            if (state.isEditMode) {
                AccountChipsRow(accounts, state.selectedAccountId, onAccountSelected)
            } else {
                val account = accounts.find { it.id == state.selectedAccountId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (account?.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = account?.name ?: "Sin cuenta",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountChipsRow(
    accounts: List<Account>,
    selectedId: Long?,
    onSelected: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { account ->
            FilterChip(
                selected = account.id == selectedId,
                onClick = { onSelected(account.id) },
                label = { Text(account.name) },
                leadingIcon = {
                    Icon(
                        imageVector = if (account.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column {
        // Opción "Sin categoría"
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Sin categoría") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconSm)
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Mismo grid de chips que usan los formularios de gasto/ingreso.
        CategoryChipsGrid(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { onCategorySelected(it) }
        )
    }
}
