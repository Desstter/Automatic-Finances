package com.example.automaticfinances.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.ui.components.common.PremiumEmptyState
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.components.common.TransactionListSkeleton
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.FinanceTypography
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    onNavigateBack: () -> Unit = {},
    onAddIncomeClick: () -> Unit = {},
    onIncomeClick: (String) -> Unit = {}
) {
    val viewModel: IncomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingresos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddIncomeClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ingreso") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.md, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                IncomesSummarySection(
                    totalIncome = state.totalIncome,
                    monthlyIncome = state.monthlyIncome,
                    incomeCount = state.incomeCount,
                    nf = nf
                )
            }

            item { SectionHeader(title = "Historial de ingresos") }

            when {
                state.isLoading -> item { TransactionListSkeleton(itemCount = 5) }
                state.incomes.isEmpty() -> item {
                    PremiumEmptyState(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = "Sin ingresos registrados",
                        description = "Los ingresos se detectan automáticamente desde tus notificaciones bancarias, o puedes agregarlos manualmente.",
                        actionLabel = "Agregar ingreso",
                        onAction = onAddIncomeClick
                    )
                }
                else -> items(state.incomes, key = { it.id }) { income ->
                    IncomeCard(income = income, nf = nf, onClick = { onIncomeClick(income.id) })
                }
            }
        }
    }
}

@Composable
private fun IncomesSummarySection(
    totalIncome: Long,
    monthlyIncome: Long,
    incomeCount: Int,
    nf: NumberFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = FinanceTheme.colors.profitContainer
    ) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Text(
                text = "Ingresos totales",
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTheme.colors.onProfitContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = nf.format(totalIncome / 100.0),
                style = FinanceTypography.moneyLarge.copy(fontSize = 30.sp),
                color = FinanceTheme.colors.onProfitContainer
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(label = "Este mes", value = nf.format(monthlyIncome / 100.0), modifier = Modifier.weight(1f))
                SummaryStat(label = "Movimientos", value = incomeCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = FinanceTheme.colors.onProfitContainer.copy(alpha = 0.8f))
            Spacer(Modifier.height(Spacing.xxs))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = FinanceTheme.colors.onProfitContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeCard(
    income: TransactionWithCategory,
    nf: NumberFormat,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.avatarMd)
                    .clip(CircleShape)
                    .background(FinanceTheme.colors.profit.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(income.categoryIcon ?: "•", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = income.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${income.categoryName ?: "Sin categoría"} · ${income.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "+ ${nf.format(income.amountCents / 100.0)}",
                style = FinanceTypography.moneySmall.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceTheme.colors.profit
            )
        }
    }
}
