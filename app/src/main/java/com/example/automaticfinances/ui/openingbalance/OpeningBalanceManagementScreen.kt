package com.example.automaticfinances.ui.openingbalance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningBalanceManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openingBalanceRepository = remember {
        OpeningBalanceRepository(
            openingBalanceDao = AppDatabase.get().openingBalanceDao(),
            accountDao = AppDatabase.get().accountDao(),
            transactionDao = AppDatabase.get().transactionDao()
        )
    }
    
    val viewModel: OpeningBalanceManagementViewModel = viewModel {
        OpeningBalanceManagementViewModel(openingBalanceRepository)
    }
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadOpeningBalanceData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Administrar Balances",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSetup) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar balances")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.accountsWithOpeningBalance.isEmpty()) {
                FloatingActionButton(
                    onClick = onNavigateToSetup
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Configurar balances")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Section
            state.summary?.let { summary ->
                item {
                    OpeningBalanceSummaryCard(summary = summary)
                }
            }
            
            // Current Status Section
            if (state.accountsWithOpeningBalance.isNotEmpty()) {
                item {
                    Text(
                        text = "Estado Actual de Cuentas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(state.accountsWithOpeningBalance) { accountWithBalance ->
                    AccountBalanceCard(
                        accountWithBalance = accountWithBalance,
                        onAccountClick = { /* Navigate to account detail */ }
                    )
                }
            }
            
            // History Section
            if (state.openingBalanceHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Historial de Cambios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(state.openingBalanceHistory.take(10)) { openingBalance ->
                    OpeningBalanceHistoryCard(
                        openingBalance = openingBalance,
                        accountName = state.accountsWithOpeningBalance
                            .find { it.account.id == openingBalance.accountId }
                            ?.account?.name ?: "Cuenta desconocida"
                    )
                }
            }
            
            // Empty State
            if (state.accountsWithOpeningBalance.isEmpty() && !state.isLoading) {
                item {
                    EmptyBalancesCard(
                        onSetupClick = onNavigateToSetup
                    )
                }
            }
            
            // Actions Section
            if (state.accountsWithOpeningBalance.isNotEmpty()) {
                item {
                    QuickActionsCard(
                        onEditBalances = onNavigateToSetup,
                        onRecalculate = { viewModel.recalculateBalances() },
                        isRecalculating = state.isRecalculating
                    )
                }
            }
            
            // Error Display
            state.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
            // Loading indicator as item in LazyColumn instead of overlay
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Cargando balances...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpeningBalanceSummaryCard(
    summary: OpeningBalanceSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resumen Financiero",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryItem(
                    label = "Balance Inicial",
                    value = summary.formattedTotalOpening,
                    icon = "💰",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                
                SummaryItem(
                    label = "Balance Actual",
                    value = summary.formattedTotalCurrent,
                    icon = "📊",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryItem(
                    label = "Cambio Neto",
                    value = summary.formattedTotalChange,
                    icon = if (summary.hasPositiveGrowth) "📈" else "📉",
                    color = if (summary.hasPositiveGrowth) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                
                SummaryItem(
                    label = "Crecimiento",
                    value = "${summary.growthPercentage.toInt()}%",
                    icon = "📊",
                    color = if (summary.hasPositiveGrowth) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
            
            summary.effectiveDate?.let { date ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "📅 Fecha efectiva: ${date}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountBalanceCard(
    accountWithBalance: AccountWithOpeningBalance,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onAccountClick
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
                Column {
                    Text(
                        text = accountWithBalance.account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (accountWithBalance.account.type) {
                            AccountType.BANK -> "💳 Cuenta bancaria"
                            AccountType.CASH -> "💵 Efectivo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = accountWithBalance.formattedCurrentBalance,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (accountWithBalance.hasOpeningBalance) {
                        Text(
                            text = "Inicial: ${accountWithBalance.formattedOpeningBalance}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (accountWithBalance.hasOpeningBalance) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cambio neto:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = accountWithBalance.formattedNetChange,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                accountWithBalance.hasGrowth -> MaterialTheme.colorScheme.primary
                                accountWithBalance.hasDecline -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = when {
                                accountWithBalance.hasGrowth -> "📈"
                                accountWithBalance.hasDecline -> "📉"
                                else -> "➖"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (accountWithBalance.transactionsSinceOpening > 0) {
                    Text(
                        text = "${accountWithBalance.transactionsSinceOpening} transacciones desde el balance inicial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "⚠️ Sin balance inicial configurado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun OpeningBalanceHistoryCard(
    openingBalance: OpeningBalance,
    accountName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = openingBalance.formattedEffectiveDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (openingBalance.note.isNotEmpty()) {
                    Text(
                        text = openingBalance.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = openingBalance.formattedBalance,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (openingBalance.isPositiveBalance) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                
                if (!openingBalance.isActive) {
                    Text(
                        text = "Inactivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBalancesCard(
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "💰",
                style = MaterialTheme.typography.displayMedium
            )
            
            Text(
                text = "No hay balances configurados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Configura tus balances iniciales para comenzar a trackear tu progreso financiero",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onSetupClick) {
                Text("Configurar Balances")
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onEditBalances: () -> Unit,
    onRecalculate: () -> Unit,
    isRecalculating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Acciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditBalances,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Editar Balances")
                }
                
                OutlinedButton(
                    onClick = onRecalculate,
                    enabled = !isRecalculating,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRecalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Recalcular")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cerrar",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}