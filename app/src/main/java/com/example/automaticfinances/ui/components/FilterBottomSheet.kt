package com.example.automaticfinances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.ui.HomeState
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    state: HomeState,
    numberFormat: NumberFormat,
    onDateFilterChange: (String?, String?) -> Unit,
    onAmountFilterChange: (Long?, Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categorías", "Fechas", "Montos", "Buscar")
    
    // Temporary state for previewing changes before applying
    var tempCategoryFilter by remember(state.selectedCategoryFilter) { mutableStateOf(state.selectedCategoryFilter ?: -1L) }
    var tempSearchQuery by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }
    var tempDateStart by remember(state.dateFilterStart) { mutableStateOf(state.dateFilterStart) }
    var tempDateEnd by remember(state.dateFilterEnd) { mutableStateOf(state.dateFilterEnd) }
    var tempMinAmount by remember(state.minAmountFilter) { mutableStateOf(state.minAmountFilter ?: -1L) }
    var tempMaxAmount by remember(state.maxAmountFilter) { mutableStateOf(state.maxAmountFilter ?: -1L) }
    
    val hasActiveFilters = tempCategoryFilter != -1L || 
                          tempSearchQuery.isNotBlank() || 
                          tempDateStart != null || 
                          tempDateEnd != null || 
                          tempMinAmount != -1L || 
                          tempMaxAmount != -1L
    
    // Calculate preview count (simplified)
    val filteredCount = state.transactions.size // TODO: Apply temp filters for real preview
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
        ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with handle and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros Avanzados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Preview counter
                if (hasActiveFilters) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$filteredCount resultados",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Tab navigation
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Category
                                        1 -> Icons.Default.CalendarMonth
                                        2 -> Icons.Default.MonetizationOn
                                        else -> Icons.Default.Check
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(title)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> CategoryFilterContent(
                        categories = state.categories,
                        selectedCategoryId = if (tempCategoryFilter == -1L) null else tempCategoryFilter,
                        onCategorySelected = { categoryId ->
                            tempCategoryFilter = categoryId ?: -1L
                        }
                    )
                    1 -> DateFilterContent(
                        startDate = tempDateStart,
                        endDate = tempDateEnd,
                        onDateRangeChange = { start, end ->
                            tempDateStart = start
                            tempDateEnd = end
                        }
                    )
                    2 -> AmountFilterContent(
                        minAmount = if (tempMinAmount == -1L) null else tempMinAmount,
                        maxAmount = if (tempMaxAmount == -1L) null else tempMaxAmount,
                        numberFormat = numberFormat,
                        onAmountRangeChange = { min, max ->
                            tempMinAmount = min ?: -1L
                            tempMaxAmount = max ?: -1L
                        }
                    )
                    3 -> SearchFilterContent(
                        searchQuery = tempSearchQuery,
                        onSearchQueryChange = { tempSearchQuery = it }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clear all button
                OutlinedButton(
                    onClick = {
                        tempCategoryFilter = -1L
                        tempSearchQuery = ""
                        tempDateStart = null
                        tempDateEnd = null
                        tempMinAmount = -1L
                        tempMaxAmount = -1L
                        onClearAllFilters()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasActiveFilters
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar")
                }
                
                // Apply button
                Button(
                    onClick = {
                        // Apply all temp filters to actual state
                        onCategoryFilterChange(if (tempCategoryFilter == -1L) null else tempCategoryFilter)
                        onSearchQueryChange(tempSearchQuery)
                        onDateFilterChange(tempDateStart, tempDateEnd)
                        onAmountFilterChange(
                            if (tempMinAmount == -1L) null else tempMinAmount,
                            if (tempMaxAmount == -1L) null else tempMaxAmount
                        )
                        onApplyFilters()
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aplicar Filtros")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterContent(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column {
        Text(
            text = "Selecciona una categoría",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // "Todas" option
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            label = { Text("🏷️ Todas las categorías") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        // Category options in a grid-like layout
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = {
                            Text("${category.icon} ${category.name}")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Fill remaining space if odd number
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateFilterContent(
    startDate: String?,
    endDate: String?,
    onDateRangeChange: (String?, String?) -> Unit
) {
    Column {
        Text(
            text = "Rango de fechas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Quick date presets
        val quickFilters = listOf(
            "Hoy" to { 
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                onDateRangeChange(today, today)
            },
            "Esta semana" to {
                val today = LocalDate.now()
                val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
                onDateRangeChange(
                    weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            },
            "Este mes" to {
                val today = LocalDate.now()
                val monthStart = today.withDayOfMonth(1)
                onDateRangeChange(
                    monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            },
            "Últimos 30 días" to {
                val today = LocalDate.now()
                val thirtyDaysAgo = today.minusDays(30)
                onDateRangeChange(
                    thirtyDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(quickFilters) { (label, action) ->
                FilterChip(
                    selected = false,
                    onClick = action,
                    label = { Text(label) }
                )
            }
        }
        
        // Current selection display
        if (startDate != null || endDate != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Rango seleccionado:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${startDate ?: "..."} → ${endDate ?: "..."}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        // Clear dates button
        OutlinedButton(
            onClick = { onDateRangeChange(null, null) },
            enabled = startDate != null || endDate != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limpiar fechas")
        }
    }
}

@Composable
private fun AmountFilterContent(
    minAmount: Long?,
    maxAmount: Long?,
    numberFormat: NumberFormat,
    onAmountRangeChange: (Long?, Long?) -> Unit
) {
    Column {
        Text(
            text = "Rango de montos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Quick amount presets
        val quickFilters = listOf(
            "< $50K" to { onAmountRangeChange(null, 5000000L) },
            "$50K - $200K" to { onAmountRangeChange(5000000L, 20000000L) },
            "> $200K" to { onAmountRangeChange(20000000L, null) },
            "> $500K" to { onAmountRangeChange(50000000L, null) }
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(quickFilters) { (label, action) ->
                FilterChip(
                    selected = false,
                    onClick = action,
                    label = { Text(label) }
                )
            }
        }
        
        // Current selection display
        if (minAmount != null || maxAmount != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Rango seleccionado:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${
                            if (minAmount != null) numberFormat.format(minAmount / 100.0) else "Sin mínimo"
                        } - ${
                            if (maxAmount != null) numberFormat.format(maxAmount / 100.0) else "Sin máximo"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        
        // Clear amounts button
        OutlinedButton(
            onClick = { onAmountRangeChange(null, null) },
            enabled = minAmount != null || maxAmount != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limpiar montos")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Buscar en transacciones",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Buscar por descripción...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "💡 Puedes buscar por nombre del comercio, descripción o cualquier texto de la transacción.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}