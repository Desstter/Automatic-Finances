package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.ui.theme.FinanceTheme
import java.text.NumberFormat
import java.util.*

@Composable
fun KPICard(
    title: String,
    currentValue: String,
    icon: ImageVector,
    subtitle: String? = null,
    trend: KPITrend? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                trend?.let {
                    TrendIndicator(trend = it)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetKPICard(
    totalBudgetCents: Long,
    totalSpentCents: Long,
    budgetsCount: Int,
    overBudgetCount: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val percentageUsed = if (totalBudgetCents == 0L) 0f
                        else (totalSpentCents.toFloat() / totalBudgetCents.toFloat()) * 100f

    val color = when {
        percentageUsed >= 100f -> MaterialTheme.colorScheme.error
        percentageUsed >= 75f  -> FinanceTheme.colors.warning
        percentageUsed >= 50f  -> MaterialTheme.colorScheme.tertiary
        else                   -> MaterialTheme.colorScheme.primary
    }

    val trend = when {
        overBudgetCount > 0    -> KPITrend.DOWN
        percentageUsed < 50f   -> KPITrend.UP
        else                   -> KPITrend.NEUTRAL
    }

    val subtitle = if (overBudgetCount > 0) {
        "$overBudgetCount presupuesto(s) excedido(s)"
    } else {
        "$budgetsCount presupuesto(s) activo(s)"
    }

    KPICard(
        title = "Presupuestos",
        currentValue = "${percentageUsed.toInt()}% usado",
        icon = Icons.Default.AccountBalanceWallet,
        subtitle = subtitle,
        trend = trend,
        color = color,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun SpendingKPICard(
    monthlySpentCents: Long,
    previousMonthSpentCents: Long? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val currentValue = nf.format(monthlySpentCents / 100.0)

    val trend = previousMonthSpentCents?.let { previous ->
        when {
            monthlySpentCents > previous -> KPITrend.UP
            monthlySpentCents < previous -> KPITrend.DOWN
            else                         -> KPITrend.NEUTRAL
        }
    }

    val subtitle = previousMonthSpentCents?.let { previous ->
        val difference = monthlySpentCents - previous
        val percentageChange = if (previous == 0L) 0f
                              else (difference.toFloat() / previous.toFloat()) * 100f
        val sign = if (difference >= 0) "+" else ""
        "$sign${nf.format(difference / 100.0)} ($sign${percentageChange.toInt()}%)"
    }

    KPICard(
        title = "Gasto del Mes",
        currentValue = currentValue,
        icon = Icons.Default.CreditCard,
        subtitle = subtitle,
        trend = trend,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TrendIndicator(trend: KPITrend) {
    val (icon, color) = when (trend) {
        KPITrend.UP      -> Icons.AutoMirrored.Filled.TrendingUp   to FinanceTheme.colors.profit
        KPITrend.DOWN    -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.error
        KPITrend.NEUTRAL -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = color
    )
}

enum class KPITrend {
    UP, DOWN, NEUTRAL
}
