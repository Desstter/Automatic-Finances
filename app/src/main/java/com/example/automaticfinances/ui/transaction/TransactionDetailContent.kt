package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
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
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
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
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = numberFormat.format(transaction.amountCents / 100.0),
                style = FinanceTypography.moneyLarge,
                color = FinanceTheme.colors.loss
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

    // Card con información básica
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = numberFormat.format(transaction.amountCents / 100.0),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = transaction.type,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${transaction.date} a las ${transaction.time}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (transaction.srcLast4 != null || transaction.dstLast4 != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Descripción",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isEditMode && !state.isTransfer) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
            } else {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    // Categoría (editable) — una transferencia no tiene categoría (no es ingreso ni gasto).
    if (!state.isTransfer) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                        Spacer(modifier = Modifier.width(8.dp))
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
    }

    // Notas (editable)
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isEditMode) {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Agregar notas...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (state.isTransfer) "Cuentas" else "Cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isTransfer) {
                if (state.isEditMode) {
                    Text("Desde", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    AccountChipsRow(accounts, state.originAccountId, onOriginSelected)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Hacia", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    AccountChipsRow(accounts, state.destAccountId, onDestSelected)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = nameOf(state.originAccountId), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                            contentDescription = "hacia",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = account?.name ?: "Sin cuenta",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
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

@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column {
        // Opción "Sin categoría"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategorySelected(null) },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedCategoryId == null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sin categoría",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categorías en grid
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier.clickable { onCategorySelected(category.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCategoryId == category.id) {
                            Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = category.icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(android.graphics.Color.parseColor(category.color))
                        )
                    }
                }
            }
        }
    }
}
