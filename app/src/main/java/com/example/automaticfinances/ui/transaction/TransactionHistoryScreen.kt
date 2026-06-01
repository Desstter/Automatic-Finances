package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.ui.CompactTransactionItem
import com.example.automaticfinances.ui.components.FilterBottomSheet
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.TransactionListSkeleton
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: (() -> Unit)? = null,
    onTransactionClick: (String) -> Unit = {}
) {
    val viewModel: TransactionHistoryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text("Historial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (state.hasActiveFilters) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Filtros",
                                tint = if (state.hasActiveFilters) MaterialTheme.colorScheme.primary
                                       else LocalContentColor.current
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatsCard(
                totalTransactions = state.filteredTransactions.size,
                totalAmount = state.filteredTransactions.sumOf { it.amountCents },
                numberFormat = numberFormat,
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md)
            )

            QuickFilterChips(
                selectedSource = state.sourceFilter,
                selectedType = state.typeFilter,
                onSourceChange = viewModel::setSourceFilter,
                onTypeChange = viewModel::setTypeFilter,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            when {
                state.isLoading -> TransactionListSkeleton(itemCount = 8, modifier = Modifier.padding(top = Spacing.sm))
                state.filteredTransactions.isEmpty() -> PremiumEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Sin transacciones",
                    description = "No se encontraron transacciones con los filtros seleccionados.",
                    actionLabel = if (state.hasActiveFilters) "Limpiar filtros" else null,
                    onAction = if (state.hasActiveFilters) viewModel::clearFilters else null
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
                ) {
                    items(state.filteredTransactions, key = { it.id }) { transaction ->
                        CompactTransactionItem(
                            transaction = transaction,
                            numberFormat = numberFormat,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            FilterBottomSheet(
                categories = state.categories,
                selectedCategoryId = state.categoryFilter,
                dateStart = state.dateStart,
                dateEnd = state.dateEnd,
                minAmount = state.minAmount,
                maxAmount = state.maxAmount,
                searchQuery = state.searchQuery,
                numberFormat = numberFormat,
                resultCount = { categoryId, search, dateStart, dateEnd, min, max ->
                    viewModel.countMatching(categoryId, search, dateStart, dateEnd, min, max)
                },
                onApply = { categoryId, search, dateStart, dateEnd, min, max ->
                    viewModel.applyAdvancedFilters(categoryId, search, dateStart, dateEnd, min, max)
                },
                onClearAll = viewModel::clearFilters,
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterChips(
    selectedSource: String?,
    selectedType: String?,
    onSourceChange: (String?) -> Unit,
    onTypeChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(selected = selectedSource == null && selectedType == null,
            onClick = { onSourceChange(null); onTypeChange(null) }, label = { Text("Todos") })
        FilterChip(selected = selectedSource == "manual",
            onClick = { onSourceChange(if (selectedSource == "manual") null else "manual") },
            label = { Text("Manuales") })
        FilterChip(selected = selectedSource == "notif",
            onClick = { onSourceChange(if (selectedSource == "notif") null else "notif") },
            label = { Text("Automáticas") })
        VerticalDivider(modifier = Modifier.height(24.dp))
        FilterChip(selected = selectedType == "COMPRA",
            onClick = { onTypeChange(if (selectedType == "COMPRA") null else "COMPRA") },
            label = { Text("Compras") })
        FilterChip(selected = selectedType == "TRANSFERENCIA",
            onClick = { onTypeChange(if (selectedType == "TRANSFERENCIA") null else "TRANSFERENCIA") },
            label = { Text("Transferencias") })
        FilterChip(selected = selectedType == "MANUAL",
            onClick = { onTypeChange(if (selectedType == "MANUAL") null else "MANUAL") },
            label = { Text("Efectivo") })
    }
}

@Composable
fun StatsCard(
    totalTransactions: Int,
    totalAmount: Long,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.card),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalTransactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Transacciones", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(40.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = numberFormat.format(totalAmount / 100.0),
                    style = FinanceTypography.moneyMedium
                )
                Text("Total", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
