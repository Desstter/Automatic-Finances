package com.example.automaticfinances.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.data.repo.TransactionWithCategory
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionHistoryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val numberFormat = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Transacciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFilters() }) {
                        Text("🔍", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Estadísticas rápidas
            StatsCard(
                totalTransactions = state.filteredTransactions.size,
                totalAmount = state.filteredTransactions.sumOf { it.amountCents },
                numberFormat = numberFormat,
                modifier = Modifier.padding(16.dp)
            )

            // Filtros (expandible)
            if (state.showFilters) {
                FiltersCard(
                    selectedSource = state.sourceFilter,
                    selectedType = state.typeFilter,
                    selectedCategory = state.categoryFilter,
                    categories = state.categories,
                    onSourceChange = viewModel::setSourceFilter,
                    onTypeChange = viewModel::setTypeFilter,
                    onCategoryChange = viewModel::setCategoryFilter,
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Lista de transacciones
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredTransactions) { transaction ->
                        TransactionHistoryItem(
                            transaction = transaction,
                            numberFormat = numberFormat
                        )
                    }

                    if (state.filteredTransactions.isEmpty() && !state.isLoading) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No se encontraron transacciones con los filtros seleccionados",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
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
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalTransactions",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Transacciones",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = numberFormat.format(totalAmount / 100.0),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall
                )
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros",
                    style = MaterialTheme.typography.titleMedium
                )
                
                TextButton(onClick = onClearFilters) {
                    Text("Limpiar")
                }
            }

            // Filtro por origen
            Text("Origen:", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = { onSourceChange(null) },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = selectedSource == "manual",
                    onClick = { onSourceChange("manual") },
                    label = { Text("✋ Manuales") }
                )
                FilterChip(
                    selected = selectedSource?.startsWith("notif") == true,
                    onClick = { onSourceChange("notif") },
                    label = { Text("🤖 Automáticas") }
                )
            }

            // Filtro por tipo
            Text("Tipo:", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeChange(null) },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = selectedType == "COMPRA",
                    onClick = { onTypeChange("COMPRA") },
                    label = { Text("Compras") }
                )
                FilterChip(
                    selected = selectedType == "TRANSFERENCIA",
                    onClick = { onTypeChange("TRANSFERENCIA") },
                    label = { Text("Transferencias") }
                )
                FilterChip(
                    selected = selectedType == "MANUAL",
                    onClick = { onTypeChange("MANUAL") },
                    label = { Text("Efectivo") }
                )
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(
    transaction: TransactionWithCategory,
    numberFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = numberFormat.format(transaction.amountCents / 100.0),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            supportingContent = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Icono de categoría
                        transaction.categoryIcon?.let { icon ->
                            Text(text = icon)
                        }
                        
                        // Nombre de categoría
                        transaction.categoryName?.let { categoryName ->
                            Text(
                                text = categoryName,
                                color = transaction.categoryColor?.let { 
                                    Color(android.graphics.Color.parseColor(it))
                                } ?: MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Fecha y hora
                        Text(
                            text = "${transaction.date} ${transaction.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Información adicional
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = transaction.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = when {
                                transaction.source?.startsWith("notif") == true -> "🤖 Automática"
                                transaction.source == "manual" -> "✋ Manual"
                                else -> "📱 SMS"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        )
    }
}