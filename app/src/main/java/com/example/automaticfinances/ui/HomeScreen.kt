package com.example.automaticfinances.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.system.SystemConfigurationChecker
import com.example.automaticfinances.ui.components.BalanceOverviewCard
import com.example.automaticfinances.ui.components.FilterBottomSheet
import com.example.automaticfinances.ui.components.FilterPreviewCard
import com.example.automaticfinances.ui.components.FilterSummary
import com.example.automaticfinances.ui.components.IntelligenceInsightsCard
import com.example.automaticfinances.ui.components.common.ExpandableBanner
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.components.common.StatusPill
import com.example.automaticfinances.ui.components.common.StatusTone
import com.example.automaticfinances.ui.components.common.TransactionListSkeleton
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    // System health monitoring with auto-hide logic
    val systemHealth by SystemConfigurationChecker.rememberSystemHealth(context)
    val shouldShowServiceStatus by remember(systemHealth, state.serviceIssuesLastFixedAt) {
        derivedStateOf {
            SystemConfigurationChecker.shouldShowServiceStatus(context, state.serviceIssuesLastFixedAt)
        }
    }

    LaunchedEffect(systemHealth.needsUserAttention) {
        if (systemHealth.needsUserAttention) {
            onServiceIssuesDetected()
        } else if (state.serviceIssuesLastFixedAt == 0L) {
            onServiceIssuesResolved()
        }
    }

    val isServiceRunning = systemHealth.isServiceRunning
    val isListenerEnabled = systemHealth.isListenerEnabled

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val activeFilters = hasActiveFilters(state)

    // Group transactions by human date bucket, preserving descending order.
    val groupedTransactions = remember(state.transactions) {
        state.transactions.groupBy { formatTransactionDate(it.date) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            EnhancedTopAppBar(
                title = "Automatic Finances",
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                hasActiveFilters = activeFilters,
                onClearFilters = onClearFilters,
                onOpenFilters = { showFilterBottomSheet = true },
                themeViewModel = themeViewModel,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(bottom = 96.dp, top = Spacing.md)
            ) {
                // Service status banner — only when issues detected / grace period
                item(key = "service") {
                    ExpandableBanner(visible = shouldShowServiceStatus) {
                        Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                            CompactServiceStatusCard(
                                isServiceRunning = isServiceRunning,
                                isListenerEnabled = isListenerEnabled,
                                onOpenNotifAccess = onOpenNotifAccess
                            )
                        }
                    }
                }

                // Balance hero
                item(key = "balance") {
                    Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
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
                }

                if (state.intelligenceActive) {
                    item(key = "intelligence") {
                        Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                            IntelligenceInsightsCard(
                                totalPreferences = state.totalPreferences,
                                overallAccuracy = state.overallAccuracy,
                                categoryStats = state.categoryAccuracyStats,
                                onViewSuggestions = {}
                            )
                        }
                    }
                }

                if (activeFilters) {
                    item(key = "filterPreview") {
                        Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                            FilterPreviewCard(
                                summary = buildFilterSummary(state, nf),
                                onClearFilter = onClearFilters
                            )
                        }
                    }
                }

                // Transactions section header
                item(key = "txHeader") {
                    SectionHeader(
                        title = if (activeFilters) "Resultados" else "Movimientos",
                        subtitle = if (state.transactions.isNotEmpty())
                            "${state.transactions.size} transacciones" else null,
                        actionLabel = if (!activeFilters && state.transactions.isNotEmpty()) "Ver historial" else null,
                        onActionClick = if (!activeFilters) onViewHistoryClick else null,
                        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)
                    )
                }

                when {
                    state.isLoading && state.transactions.isEmpty() -> {
                        item(key = "skeleton") { TransactionListSkeleton(itemCount = 7) }
                    }

                    state.transactions.isEmpty() -> {
                        item(key = "empty") {
                            if (activeFilters) {
                                PremiumEmptyState(
                                    icon = Icons.Default.Search,
                                    title = "Sin resultados",
                                    description = "No encontramos transacciones con los filtros aplicados. Prueba ajustarlos.",
                                    actionLabel = "Limpiar filtros",
                                    onAction = onClearFilters
                                )
                            } else {
                                PremiumEmptyState(
                                    icon = Icons.Default.ReceiptLong,
                                    title = "Aún no hay movimientos",
                                    description = "Tus transacciones aparecerán aquí automáticamente al detectarlas, o regístralas manualmente.",
                                    actionLabel = "Registrar transacción",
                                    onAction = onAddTransactionClick
                                )
                            }
                        }
                    }

                    else -> {
                        groupedTransactions.forEach { (dateLabel, txs) ->
                            stickyHeader(key = "header_$dateLabel") {
                                TransactionDateHeader(dateLabel)
                            }
                            items(txs, key = { it.id }) { tx ->
                                CompactTransactionItem(
                                    transaction = tx,
                                    numberFormat = nf,
                                    onClick = { onTransactionClick(tx.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

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
                onApplyFilters = {},
                onDismiss = { showFilterBottomSheet = false }
            )
        }
    }
}

@Composable
private fun TransactionDateHeader(label: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
        )
    }
}

@Composable
fun CompactTransactionItem(
    transaction: TransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(transaction.categoryColor) {
        transaction.categoryColor?.let {
            runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category avatar — tinted by category color
        Box(
            modifier = Modifier
                .size(Sizes.avatarMd)
                .clip(CircleShape)
                .background(
                    (categoryColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.14f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.categoryIcon ?: "•",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        transaction.source.startsWith("notif") -> Icons.Default.SmartToy
                        transaction.source == "manual" -> Icons.Default.Edit
                        else -> Icons.Default.PhoneAndroid
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = buildString {
                        append(transaction.categoryName ?: "Sin categoría")
                        append(" · ")
                        append(transaction.time)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(Spacing.sm))

        Text(
            text = buildString {
                when {
                    transaction.isIncome -> append("+ ")
                    transaction.type == "TRANSFERENCIA" -> {}
                    else -> append("− ")
                }
                append(numberFormat.format(transaction.amountCents / 100.0))
            },
            style = FinanceTypography.moneySmall.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                transaction.isIncome -> FinanceTheme.colors.profit
                transaction.type == "TRANSFERENCIA" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

// Human-friendly date bucketing used for grouping & headers.
private fun formatTransactionDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)
        when {
            date == today -> "Hoy"
            date == yesterday -> "Ayer"
            date.isAfter(today.minusWeeks(1)) ->
                date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", Locale("es", "ES")))
                    .replaceFirstChar { it.uppercase() }
            date.year == today.year ->
                date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM", Locale("es", "ES")))
                    .replaceFirstChar { it.uppercase() }
            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES")))
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
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    onOpenFilters: () -> Unit = {},
    themeViewModel: com.example.automaticfinances.ui.theme.ThemeViewModel? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }

    if (isSearchActive) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar transacciones…") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
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
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                themeViewModel?.let { viewModel ->
                    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                    val themeIcon = when (themeMode) {
                        com.example.automaticfinances.data.preferences.ThemeMode.LIGHT -> Icons.Default.BrightnessHigh
                        com.example.automaticfinances.data.preferences.ThemeMode.DARK -> Icons.Default.Brightness3
                        com.example.automaticfinances.data.preferences.ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(themeIcon, contentDescription = "Cambiar tema")
                    }
                }

                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }

                IconButton(onClick = onOpenFilters) {
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
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
    val tone = when {
        isServiceRunning && isListenerEnabled -> StatusTone.Positive
        isListenerEnabled && !isServiceRunning -> StatusTone.Warning
        else -> StatusTone.Critical
    }
    val container = when (tone) {
        StatusTone.Positive -> MaterialTheme.colorScheme.surfaceContainer
        StatusTone.Warning -> FinanceTheme.colors.warningContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = container
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isServiceRunning && isListenerEnabled -> "Detección activa"
                        isListenerEnabled && !isServiceRunning -> "Iniciando servicio…"
                        else -> "Configura la detección automática"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = when {
                        isServiceRunning && isListenerEnabled -> "Monitoreando notificaciones bancarias"
                        isListenerEnabled && !isServiceRunning -> "Conectando con el sistema"
                        else -> "Otorga acceso a notificaciones para registrar gastos solo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Spacing.md))

            if (!isListenerEnabled) {
                FilledTonalButton(onClick = onOpenNotifAccess) {
                    Text("Activar")
                }
            } else {
                StatusPill(
                    label = if (isServiceRunning) "Activo" else "…",
                    tone = tone
                )
            }
        }
    }
}

private fun buildFilterSummary(state: HomeState, nf: NumberFormat): FilterSummary {
    val category = state.categories.find { it.id == state.selectedCategoryFilter }
    return FilterSummary(
        categoryName = category?.name,
        categoryIcon = category?.icon,
        dateRange = if (state.dateFilterStart != null || state.dateFilterEnd != null) {
            "${state.dateFilterStart ?: "…"} - ${state.dateFilterEnd ?: "…"}"
        } else null,
        amountRange = if (state.minAmountFilter != null || state.maxAmountFilter != null) {
            val min = state.minAmountFilter
            val max = state.maxAmountFilter
            "${if (min != null) nf.format(min / 100.0) else "Sin mínimo"} - ${if (max != null) nf.format(max / 100.0) else "Sin máximo"}"
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
    )
}

private fun hasActiveFilters(state: HomeState): Boolean {
    return state.selectedCategoryFilter != null ||
        state.searchQuery.isNotBlank() ||
        state.dateFilterStart != null ||
        state.dateFilterEnd != null ||
        state.minAmountFilter != null ||
        state.maxAmountFilter != null
}
