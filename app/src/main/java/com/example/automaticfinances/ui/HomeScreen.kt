package com.example.automaticfinances.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.ModalBottomSheet
import com.example.automaticfinances.data.repo.TransactionWithCategory
import kotlinx.coroutines.launch
import com.example.automaticfinances.system.ServiceManager
import com.example.automaticfinances.system.SystemConfigurationChecker
import com.example.automaticfinances.ui.components.IntelligenceInsightsCard
import com.example.automaticfinances.ui.components.BalanceOverviewCard
import com.example.automaticfinances.ui.components.FilterBottomSheet
import com.example.automaticfinances.ui.components.FilterPreviewCard
import com.example.automaticfinances.ui.components.FilterSummary
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stateFlow: StateFlow<HomeState>,
    themeViewModel: com.example.automaticfinances.ui.theme.ThemeViewModel? = null,
    onOpenNotifAccess: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onManageCategoriesClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    onViewInsightsClick: () -> Unit = {},
    onViewIncomesClick: () -> Unit = {},
    onViewBalancesClick: () -> Unit = {},
    onBankBalanceClick: () -> Unit = {},
    onCashBalanceClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onToggleFilters: () -> Unit = {},
    onClearFilters: () -> Unit = {},
    onDateFilterChange: (String?, String?) -> Unit = { _, _ -> },
    onAmountFilterChange: (Long?, Long?) -> Unit = { _, _ -> },
    onCategoryFilterChange: (Long?) -> Unit = {},
    onServiceIssuesResolved: () -> Unit = {},
    onServiceIssuesDetected: () -> Unit = {}
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // BottomSheet state for advanced filters
    val bottomSheetState = rememberBottomSheetScaffoldState()
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    
    val nf = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }
    
    // System health monitoring with auto-hide logic
    val systemHealth by SystemConfigurationChecker.rememberSystemHealth(context)
    val shouldShowServiceStatus by remember(systemHealth, state.serviceIssuesLastFixedAt) {
        derivedStateOf { 
            SystemConfigurationChecker.shouldShowServiceStatus(context, state.serviceIssuesLastFixedAt)
        }
    }
    
    // Track service issues and call appropriate callbacks
    LaunchedEffect(systemHealth.needsUserAttention) {
        if (systemHealth.needsUserAttention) {
            onServiceIssuesDetected()
        } else {
            // Only mark as resolved if it was previously problematic
            if (state.serviceIssuesLastFixedAt == 0L) {
                onServiceIssuesResolved()
            }
        }
    }
    
    // Legacy service status for backward compatibility
    val isServiceRunning = systemHealth.isServiceRunning
    val isListenerEnabled = systemHealth.isListenerEnabled
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { 
            EnhancedTopAppBar(
                title = "Automatic Finances",
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                showFilters = state.showFilters,
                onToggleFilters = onToggleFilters,
                hasActiveFilters = hasActiveFilters(state),
                onClearFilters = onClearFilters,
                onOpenFilters = { showFilterBottomSheet = true },
                themeViewModel = themeViewModel,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick
            ) {
                Text("💵")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .statusBarsPadding()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Service Status Card (only when issues detected or grace period)
                if (shouldShowServiceStatus) {
                    item {
                        CompactServiceStatusCard(
                            isServiceRunning = isServiceRunning,
                            isListenerEnabled = isListenerEnabled,
                            onOpenNotifAccess = onOpenNotifAccess
                        )
                    }
                }
                
                // Balance Overview Card - Bank, Cash, Total balances
                item {
                    BalanceOverviewCard(
                        bankBalanceCents = state.bankBalanceCents,
                        cashBalanceCents = state.cashBalanceCents,
                        totalBalanceCents = state.totalBalanceCents,
                        monthlyIncome = state.monthlyIncome,
                        monthlyExpenses = state.monthlyExpenses,
                        numberFormat = nf,
                        onBankClick = onBankBalanceClick,
                        onCashClick = onCashBalanceClick,
                        onViewHistoryClick = onViewHistoryClick,
                        onViewBalancesClick = onViewBalancesClick
                    )
                }
                
                // Intelligence Insights Card (only if intelligence is active)
                if (state.intelligenceActive) {
                    item {
                        IntelligenceInsightsCard(
                            totalPreferences = state.totalPreferences,
                            overallAccuracy = state.overallAccuracy,
                            categoryStats = state.categoryAccuracyStats,
                            onViewSuggestions = {
                                // Navigate to suggestions screen - será implementado
                            }
                        )
                    }
                }
                
                // Filter Preview Card (always show if filters active)
                if (hasActiveFilters(state)) {
                    item {
                        FilterPreviewCard(
                            summary = FilterSummary(
                                categoryName = state.categories.find { it.id == state.selectedCategoryFilter }?.name,
                                categoryIcon = state.categories.find { it.id == state.selectedCategoryFilter }?.icon,
                                dateRange = if (state.dateFilterStart != null || state.dateFilterEnd != null) {
                                    "${state.dateFilterStart ?: "..."} - ${state.dateFilterEnd ?: "..."}"
                                } else null,
                                amountRange = if (state.minAmountFilter != null || state.maxAmountFilter != null) {
                                    val minAmount = state.minAmountFilter
                                    val maxAmount = state.maxAmountFilter
                                    "${if (minAmount != null) nf.format(minAmount / 100.0) else "Sin mínimo"} - ${if (maxAmount != null) nf.format(maxAmount / 100.0) else "Sin máximo"}"
                                } else null,
                                searchQuery = state.searchQuery.takeIf { it.isNotBlank() },
                                totalFilters = listOfNotNull(
                                    state.selectedCategoryFilter?.toString(),
                                    state.searchQuery.takeIf { it.isNotBlank() },
                                    state.dateFilterStart,
                                    state.dateFilterEnd,
                                    state.minAmountFilter?.toString(),
                                    state.maxAmountFilter?.toString()
                                ).size,
                                resultCount = state.transactions.size
                            ),
                            onClearFilter = onClearFilters
                        )
                    }
                }
                
                // Transactions or Empty State
                if (state.transactions.isEmpty() && !state.isLoading) {
                    item {
                        EmptyTransactionsState(
                            hasFilters = hasActiveFilters(state),
                            onClearFilters = onClearFilters
                        )
                    }
                } else {
                    items(state.transactions) { tx ->
                        CompactTransactionItem(
                            transaction = tx,
                            numberFormat = nf,
                            onClick = { onTransactionClick(tx.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Advanced Filter Bottom Sheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            FilterBottomSheet(
                state = state,
                numberFormat = nf,
                onDateFilterChange = onDateFilterChange,
                onAmountFilterChange = onAmountFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onSearchQueryChange = onSearchQueryChange,
                onClearAllFilters = onClearFilters,
                onApplyFilters = { /* Filters are applied automatically through callbacks */ },
                onDismiss = { showFilterBottomSheet = false }
            )
        }
    }
}

@Composable
fun CompactTransactionItem(
    transaction: TransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Category icon + Description
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.categoryIcon ?: "💳",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatTransactionDate(transaction.date)} • ${transaction.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Right: Amount + Type badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = numberFormat.format(transaction.amountCents / 100.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (transaction.type) {
                        "COMPRA" -> MaterialTheme.colorScheme.primary
                        "TRANSFERENCIA" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            transaction.source?.startsWith("notif") == true -> "🤖"
                            transaction.source == "manual" -> "✋"
                            else -> "📱"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row: Merchant/Description and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Transaction type badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Badge(
                            containerColor = when (transaction.type) {
                                "COMPRA" -> MaterialTheme.colorScheme.secondaryContainer
                                "TRANSFERENCIA" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = transaction.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (transaction.type) {
                                    "COMPRA" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    "TRANSFERENCIA" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        
                        // Source indicator
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                transaction.source?.startsWith("notif") == true -> "🤖 Auto"
                                transaction.source == "manual" -> "✋ Manual"
                                else -> "📱 App"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Amount with emphasis
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = numberFormat.format(transaction.amountCents / 100.0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.type) {
                            "COMPRA" -> MaterialTheme.colorScheme.primary
                            "TRANSFERENCIA" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    // Account indicator if available
                    transaction.srcLast4?.let { last4 ->
                        Text(
                            text = "*$last4",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom row: Category and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with icon and color
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    transaction.categoryIcon?.let { icon ->
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    transaction.categoryName?.let { categoryName ->
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = transaction.categoryColor?.let { 
                                Color(android.graphics.Color.parseColor(it))
                            } ?: MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } ?: Text(
                        text = "Sin categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                
                // Date and time with modern formatting
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatTransactionDate(transaction.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = transaction.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Helper function to format transaction date in a user-friendly way
private fun formatTransactionDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)
        
        when {
            date == today -> "Hoy"
            date == yesterday -> "Ayer"
            date.isAfter(today.minusWeeks(1)) -> {
                date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale("es", "ES")))
                    .replaceFirstChar { it.uppercase() }
            }
            date.year == today.year -> {
                date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale("es", "ES")))
            }
            else -> {
                date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"))
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopAppBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    onOpenFilters: () -> Unit = {},
    themeViewModel: com.example.automaticfinances.ui.theme.ThemeViewModel? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    if (isSearchActive) {
        // Search mode


        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar transacciones...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = { 
                    isSearchActive = false
                    onSearchQueryChange("")
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancelar búsqueda")
                }
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        // Normal mode
        TopAppBar(
            title = { 
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                ) 
            },
            actions = {
                // Theme toggle button
                themeViewModel?.let { viewModel ->
                    val themeMode by viewModel.themeMode.collectAsState()
                    val themeIcon = when (themeMode) {
                        com.example.automaticfinances.data.preferences.ThemeMode.LIGHT -> Icons.Default.BrightnessHigh
                        com.example.automaticfinances.data.preferences.ThemeMode.DARK -> Icons.Default.Brightness3
                        com.example.automaticfinances.data.preferences.ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                    }
                    
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            themeIcon, 
                            contentDescription = "Cambiar tema",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Search button
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
                
                // Filter button
                IconButton(onClick = onOpenFilters) {
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
                
                // Clear filters button (only show if there are active filters)
                if (hasActiveFilters) {
                    IconButton(onClick = onClearFilters) {
                        Icon(
                            Icons.Default.Clear, 
                            contentDescription = "Limpiar filtros",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
fun CompactServiceStatusCard(
    isServiceRunning: Boolean,
    isListenerEnabled: Boolean,
    onOpenNotifAccess: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isServiceRunning && isListenerEnabled -> MaterialTheme.colorScheme.primaryContainer
                isListenerEnabled && !isServiceRunning -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when {
                        isServiceRunning && isListenerEnabled -> "🟢"
                        isListenerEnabled && !isServiceRunning -> "🟡"
                        else -> "🔴"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            isServiceRunning && isListenerEnabled -> "Sistema Activo"
                            isListenerEnabled && !isServiceRunning -> "Iniciando..."
                            else -> "Configurar"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            isServiceRunning && isListenerEnabled -> "Monitoreando SMS"
                            isListenerEnabled && !isServiceRunning -> "Conectando..."
                            else -> "Acceso requerido"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (!isListenerEnabled) {
                Button(
                    onClick = onOpenNotifAccess,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Configurar", style = MaterialTheme.typography.labelMedium)
                }
            } else if (isServiceRunning) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("OK", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun CompactMonthlyTotalCard(
    totalAmount: Long,
    numberFormat: NumberFormat,
    currentMonth: String,
    onViewHistoryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewHistoryClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = numberFormat.format(totalAmount / 100.0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ver más",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CompactFilterSection(
    state: HomeState,
    numberFormat: NumberFormat,
    onDateFilterChange: (String?, String?) -> Unit,
    onAmountFilterChange: (Long?, Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "🔍 Filtros Activos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick Category Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                        selected = state.selectedCategoryFilter == null
                    )
                }
                
                items(state.categories.take(4)) { category ->
                    FilterChip(
                        onClick = { onCategoryFilterChange(category.id) },
                        label = { 
                            Text(
                                text = "${category.icon} ${category.name}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = state.selectedCategoryFilter == category.id
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick Date and Amount Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { 
                            val today = java.time.LocalDate.now()
                            val weekAgo = today.minusWeeks(1)
                            onDateFilterChange(
                                weekAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            )
                        },
                        label = { Text("Semana", style = MaterialTheme.typography.labelSmall) },
                        selected = false
                    )
                }
                
                item {
                    FilterChip(
                        onClick = { onAmountFilterChange(null, 5000000) },
                        label = { Text("< $50K", style = MaterialTheme.typography.labelSmall) },
                        selected = state.maxAmountFilter == 5000000L
                    )
                }
                
                item {
                    FilterChip(
                        onClick = { onAmountFilterChange(20000000, null) },
                        label = { Text("> $200K", style = MaterialTheme.typography.labelSmall) },
                        selected = state.minAmountFilter == 20000000L
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceStatusDetails(
    isServiceRunning: Boolean,
    isListenerEnabled: Boolean
) {
    Column {
        // Notification Listener Status
        StatusDetailRow(
            icon = "🔔",
            label = "Acceso a Notificaciones",
            status = if (isListenerEnabled) "Habilitado" else "Deshabilitado",
            isOk = isListenerEnabled
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Foreground Service Status
        StatusDetailRow(
            icon = "⚙️",
            label = "Servicio de Monitoreo",
            status = if (isServiceRunning) "Ejecutándose" else "Detenido",
            isOk = isServiceRunning
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Overall functionality
        StatusDetailRow(
            icon = "🤖",
            label = "Detección Automática",
            status = if (isServiceRunning && isListenerEnabled) "Funcional" else "No Disponible",
            isOk = isServiceRunning && isListenerEnabled
        )
    }
}

@Composable
fun StatusDetailRow(
    icon: String,
    label: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        Badge(
            containerColor = if (isOk) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun FilterSection(
    state: HomeState,
    numberFormat: NumberFormat,
    onDateFilterChange: (String?, String?) -> Unit,
    onAmountFilterChange: (Long?, Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔍 Filtros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category Filter
            CategoryFilterChips(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryFilter,
                onCategorySelected = onCategoryFilterChange
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date Range Filter
            DateRangeFilter(
                startDate = state.dateFilterStart,
                endDate = state.dateFilterEnd,
                onDateRangeChange = onDateFilterChange
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Amount Range Filter
            AmountRangeFilter(
                minAmount = state.minAmountFilter,
                maxAmount = state.maxAmountFilter,
                numberFormat = numberFormat,
                onAmountRangeChange = onAmountFilterChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterChips(
    categories: List<com.example.automaticfinances.data.db.Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column {
        Text(
            text = "Categorías",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Todas" chip
            item {
                FilterChip(
                    onClick = { onCategorySelected(null) },
                    label = { Text("Todas") },
                    selected = selectedCategoryId == null
                )
            }
            
            // Category chips
            items(categories) { category ->
                FilterChip(
                    onClick = { onCategorySelected(category.id) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(category.name)
                        }
                    },
                    selected = selectedCategoryId == category.id
                )
            }
        }
    }
}

@Composable
fun DateRangeFilter(
    startDate: String?,
    endDate: String?,
    onDateRangeChange: (String?, String?) -> Unit
) {
    Column {
        Text(
            text = "Rango de Fechas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Quick date filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { 
                        val today = java.time.LocalDate.now()
                        val weekAgo = today.minusWeeks(1)
                        onDateRangeChange(
                            weekAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        )
                    },
                    label = { Text("Última semana") },
                    selected = false
                )
            }
            
            item {
                FilterChip(
                    onClick = { 
                        val today = java.time.LocalDate.now()
                        val monthStart = today.withDayOfMonth(1)
                        onDateRangeChange(
                            monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        )
                    },
                    label = { Text("Este mes") },
                    selected = false
                )
            }
            
            item {
                FilterChip(
                    onClick = { onDateRangeChange(null, null) },
                    label = { Text("Limpiar fechas") },
                    selected = startDate == null && endDate == null
                )
            }
        }
        
        // Current filter display
        if (startDate != null || endDate != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Filtro: ${startDate ?: "..."} - ${endDate ?: "..."}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AmountRangeFilter(
    minAmount: Long?,
    maxAmount: Long?,
    numberFormat: NumberFormat,
    onAmountRangeChange: (Long?, Long?) -> Unit
) {
    Column {
        Text(
            text = "Rango de Montos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Quick amount filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { onAmountRangeChange(null, 5000000) }, // < $50,000
                    label = { Text("< $50K") },
                    selected = maxAmount == 5000000L
                )
            }
            
            item {
                FilterChip(
                    onClick = { onAmountRangeChange(5000000, 20000000) }, // $50K - $200K
                    label = { Text("$50K - $200K") },
                    selected = minAmount == 5000000L && maxAmount == 20000000L
                )
            }
            
            item {
                FilterChip(
                    onClick = { onAmountRangeChange(20000000, null) }, // > $200K
                    label = { Text("> $200K") },
                    selected = minAmount == 20000000L
                )
            }
            
            item {
                FilterChip(
                    onClick = { onAmountRangeChange(null, null) },
                    label = { Text("Limpiar montos") },
                    selected = minAmount == null && maxAmount == null
                )
            }
        }
        
        // Current filter display
        if (minAmount != null || maxAmount != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Filtro: ${
                    if (minAmount != null) numberFormat.format(minAmount / 100.0) else "..."
                } - ${
                    if (maxAmount != null) numberFormat.format(maxAmount / 100.0) else "..."
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyTransactionsState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasFilters) "🔍" else "💳",
                style = MaterialTheme.typography.displaySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (hasFilters) {
                    "No se encontraron transacciones"
                } else {
                    "No hay transacciones aún"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (hasFilters) {
                    "Intenta ajustar los filtros"
                } else {
                    "Las transacciones aparecerán automáticamente"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            if (hasFilters) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClearFilters,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpiar Filtros")
                }
            }
        }
    }
}

@Composable
fun MonthlyBalanceCard(
    monthlyIncome: Long,
    monthlyExpenses: Long,
    monthlyBalance: Long,
    numberFormat: NumberFormat,
    currentMonth: String,
    onViewHistoryClick: () -> Unit,
    onViewIncomesClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (monthlyBalance >= 0) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance ${currentMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = numberFormat.format(monthlyBalance / 100.0),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (monthlyBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                
                Text(
                    text = if (monthlyBalance >= 0) "📈" else "📉",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Income and Expenses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = onViewIncomesClick,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💰",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = numberFormat.format(monthlyIncome / 100.0),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                // Expenses Card
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = onViewHistoryClick,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💸",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = numberFormat.format(monthlyExpenses / 100.0),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to check if there are active filters
private fun hasActiveFilters(state: HomeState): Boolean {
    return state.selectedCategoryFilter != null ||
           state.searchQuery.isNotBlank() ||
           state.dateFilterStart != null ||
           state.dateFilterEnd != null ||
           state.minAmountFilter != null ||
           state.maxAmountFilter != null
}