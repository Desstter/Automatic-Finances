package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.automaticfinances.ui.components.common.ExpandableBanner
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.TransactionListSkeleton
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    val viewModel: TransactionHistoryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                    IconButton(onClick = { viewModel.toggleFilters() }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filtros",
                            tint = if (state.showFilters) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
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

            ExpandableBanner(visible = state.showFilters) {
                FiltersCard(
                    selectedSource = state.sourceFilter,
                    selectedType = state.typeFilter,
                    selectedCategory = state.categoryFilter,
                    categories = state.categories,
                    onSourceChange = viewModel::setSourceFilter,
                    onTypeChange = viewModel::setTypeFilter,
                    onCategoryChange = viewModel::setCategoryFilter,
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
            }

            when {
                state.isLoading -> TransactionListSkeleton(itemCount = 8, modifier = Modifier.padding(top = Spacing.sm))
                state.filteredTransactions.isEmpty() -> PremiumEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Sin transacciones",
                    description = "No se encontraron transacciones con los filtros seleccionados."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
                ) {
                    items(state.filteredTransactions, key = { it.id }) { transaction ->
                        CompactTransactionItem(
                            transaction = transaction,
                            numberFormat = numberFormat,
                            onClick = {}
                        )
                    }
                }
            }
        }
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

@Composable
fun FiltersCard(
    selectedSource: String?,
    selectedType: String?,
    selectedCategory: Long?,
    categories: List<com.example.automaticfinances.data.db.Category>,
    onSourceChange: (String?) -> Unit,
    onTypeChange: (String?) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClearFilters) { Text("Limpiar") }
            }

            Text("Origen", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(selected = selectedSource == null, onClick = { onSourceChange(null) }, label = { Text("Todos") })
                FilterChip(selected = selectedSource == "manual", onClick = { onSourceChange("manual") }, label = { Text("Manuales") })
                FilterChip(selected = selectedSource?.startsWith("notif") == true, onClick = { onSourceChange("notif") }, label = { Text("Automáticas") })
            }

            Text("Tipo", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(selected = selectedType == null, onClick = { onTypeChange(null) }, label = { Text("Todos") })
                FilterChip(selected = selectedType == "COMPRA", onClick = { onTypeChange("COMPRA") }, label = { Text("Compras") })
                FilterChip(selected = selectedType == "TRANSFERENCIA", onClick = { onTypeChange("TRANSFERENCIA") }, label = { Text("Transferencias") })
                FilterChip(selected = selectedType == "MANUAL", onClick = { onTypeChange("MANUAL") }, label = { Text("Efectivo") })
            }
        }
    }
}
