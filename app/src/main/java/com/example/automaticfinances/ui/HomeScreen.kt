package com.example.automaticfinances.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
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
import com.example.automaticfinances.ui.components.IntelligenceInsightsCard
import com.example.automaticfinances.ui.components.SpeedDialAction
import com.example.automaticfinances.ui.components.SpeedDialFab
import com.example.automaticfinances.ui.components.SpeedDialScrim
import com.example.automaticfinances.ui.components.common.ExpandableBanner
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.components.common.TransactionListSkeleton
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.MotionTokens
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

/** How many recent movements the dashboard shows before deferring to the full history. */
private const val RECENT_LIMIT = 15

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    stateFlow: StateFlow<HomeState>,
    onOpenNotifAccess: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onAddVoiceClick: () -> Unit = {},
    onAddIncomeClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    onBankBalanceClick: () -> Unit = {},
    onCashBalanceClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var fabExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    // System health — the banner only surfaces when detection needs the user (permission off).
    val systemHealth by SystemConfigurationChecker.rememberSystemHealth(context)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // While searching, Home behaves as a search surface (all matches). Otherwise it's a
    // dashboard showing only the most recent movements, deferring the full list to Movimientos.
    val displayedTransactions = remember(state.transactions, isSearchActive) {
        if (isSearchActive) state.transactions else state.transactions.take(RECENT_LIMIT)
    }
    val groupedTransactions = remember(displayedTransactions) {
        displayedTransactions.groupBy { formatTransactionDate(it.date) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            EnhancedTopAppBar(
                title = "Automatic Finances",
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            SpeedDialFab(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                actions = listOf(
                    SpeedDialAction(
                        label = "Voz",
                        icon = Icons.Default.Mic,
                        onClick = onAddVoiceClick
                    ),
                    SpeedDialAction(
                        label = "Ingreso",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        onClick = onAddIncomeClick
                    ),
                    SpeedDialAction(
                        label = "Gasto",
                        icon = Icons.Default.Edit,
                        onClick = onAddTransactionClick
                    )
                )
            )
        }
    ) { padding ->
      Box(modifier = Modifier.padding(padding)) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(bottom = 96.dp, top = Spacing.md)
            ) {
                // The balance hero / status / intelligence cards are hidden while searching so
                // the field and matching results take the full screen.
                if (!isSearchActive) {
                    item(key = "service") {
                        ExpandableBanner(visible = systemHealth.needsUserAttention) {
                            Box(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                                CompactServiceStatusCard(onOpenNotifAccess = onOpenNotifAccess)
                            }
                        }
                    }

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
                                onViewHistoryClick = onViewHistoryClick
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
                }

                item(key = "txHeader") {
                    SectionHeader(
                        title = if (isSearchActive) "Resultados" else "Movimientos recientes",
                        subtitle = if (displayedTransactions.isNotEmpty())
                            "${displayedTransactions.size} transacciones" else null,
                        actionLabel = if (!isSearchActive && state.transactions.isNotEmpty()) "Ver historial" else null,
                        onActionClick = if (!isSearchActive) onViewHistoryClick else null,
                        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)
                    )
                }

                when {
                    state.isLoading && state.transactions.isEmpty() -> {
                        item(key = "skeleton") { TransactionListSkeleton(itemCount = 7) }
                    }

                    displayedTransactions.isEmpty() -> {
                        item(key = "empty") {
                            if (isSearchActive) {
                                PremiumEmptyState(
                                    icon = Icons.Default.Search,
                                    title = "Sin resultados",
                                    description = "No encontramos transacciones para esa búsqueda. Prueba con otro término.",
                                    actionLabel = "Limpiar búsqueda",
                                    onAction = { onSearchQueryChange("") }
                                )
                            } else {
                                PremiumEmptyState(
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
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
                                    modifier = Modifier.animateItem(
                                        placementSpec = MotionTokens.expressiveSpatialDefault()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        SpeedDialScrim(
            visible = fabExpanded,
            onDismiss = { fabExpanded = false }
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
                    // The icon encodes the entry source, which isn't repeated in the text, so it
                    // carries meaning for screen readers.
                    contentDescription = when {
                        transaction.source.startsWith("notif") -> "Detectado automáticamente"
                        transaction.source == "manual" -> "Entrada manual"
                        else -> "Otro origen"
                    },
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
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
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
                    onSearchActiveChange(false)
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
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Service status card — only rendered while [com.example.automaticfinances.system.SystemHealthStatus.needsUserAttention]
 * is true, i.e. the notification-listener permission is not yet granted. It never advertises the
 * healthy "active" state (that lives in Ajustes). Detection goes live the moment the permission is
 * granted, so there is no transient "connecting" state to show.
 */
@Composable
fun CompactServiceStatusCard(
    onOpenNotifAccess: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer
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
                    text = "Configura la detección automática",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = "Otorga acceso a las notificaciones para registrar tus gastos automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Spacing.md))

            FilledTonalButton(onClick = onOpenNotifAccess) {
                Text("Activar")
            }
        }
    }
}
