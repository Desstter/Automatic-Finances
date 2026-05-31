package com.example.automaticfinances.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.BudgetAlertLevel
import com.example.automaticfinances.data.models.BudgetComparison
import com.example.automaticfinances.data.models.BarChartItem
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetComparisonChart(
    budgetComparisons: List<BudgetComparison>,
    modifier: Modifier = Modifier,
    onBudgetClick: (BudgetComparison) -> Unit = {}
) {
    if (budgetComparisons.isEmpty()) {
        EmptyBudgetChart(modifier = modifier)
        return
    }
    
    val sortedData = remember(budgetComparisons) {
        budgetComparisons.sortedByDescending { it.utilizationPercentage }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart Header
        BudgetChartHeader(
            budgetComparisons = sortedData,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Horizontal Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sortedData.forEach { budgetComparison ->
                    BudgetComparisonBar(
                        budgetComparison = budgetComparison,
                        onClick = { onBudgetClick(budgetComparison) }
                    )
                }
            }
        }
        
        // Chart Legend
        BudgetChartLegend(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyBudgetChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💰",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Sin presupuestos activos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Crea presupuestos para ver comparaciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetChartHeader(
    budgetComparisons: List<BudgetComparison>,
    modifier: Modifier = Modifier
) {
    val overBudgetCount = budgetComparisons.count { it.isOverBudget }
    val criticalCount = budgetComparisons.count { 
        it.budgetStatus.alertLevel == BudgetAlertLevel.CRITICAL 
    }
    val safeCount = budgetComparisons.count {
        it.budgetStatus.alertLevel == BudgetAlertLevel.SAFE
    }
    val chartColors = ChartUtils.rememberChartColors()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Comparación de Presupuestos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${budgetComparisons.size} categorías activas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (overBudgetCount > 0) {
                StatusIndicator(
                    count = overBudgetCount,
                    label = "Excedido",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (criticalCount > 0) {
                StatusIndicator(
                    count = criticalCount,
                    label = "Crítico",
                    color = chartColors.critical
                )
            }
            if (safeCount > 0) {
                StatusIndicator(
                    count = safeCount,
                    label = "Seguro",
                    color = chartColors.safe
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = color,
                    size = size,
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetComparisonBar(
    budgetComparison: BudgetComparison,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val budget = budgetComparison.budgetStatus.budget
    val spent = budgetComparison.budgetStatus.currentSpentCents
    val alertLevel = budgetComparison.budgetStatus.alertLevel
    val chartColors = ChartUtils.rememberChartColors()
    val barBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val barColor = when (alertLevel) {
        BudgetAlertLevel.SAFE -> chartColors.safe
        BudgetAlertLevel.WARNING -> chartColors.warning
        BudgetAlertLevel.CRITICAL -> chartColors.critical
        BudgetAlertLevel.OVER_BUDGET -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = budgetComparison.budgetStatus.category.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = budgetComparison.budgetStatus.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "${budgetComparison.utilizationPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawBudgetComparisonBar(
                        budgetAmountCents = budget.limitAmountCents,
                        spentAmountCents = spent,
                        barColor = barColor,
                        backgroundColor = barBackground,
                        errorColor = chartColors.error,
                        canvasSize = size
                    )
                }
            }
            
            // Amount labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gastado: ${nf.format(spent / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Límite: ${nf.format(budget.limitAmountCents / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Remaining amount or overflow
            val remaining = budget.limitAmountCents - spent
            if (remaining >= 0) {
                Text(
                    text = "Restante: ${nf.format(remaining / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = chartColors.safe,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Excedido por: ${nf.format((-remaining) / 100.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Projection warning
            if (budgetComparison.budgetStatus.daysLeftInMonth > 0 && 
                budgetComparison.budgetStatus.projectedSpentCents > budget.limitAmountCents) {
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "⚠️", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Proyección: ${nf.format(budgetComparison.budgetStatus.projectedSpentCents / 100.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetChartLegend(
    modifier: Modifier = Modifier
) {
    val chartColors = ChartUtils.rememberChartColors()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = chartColors.safe,
            label = "Seguro (< 50%)",
            modifier = Modifier.weight(1f)
        )
        LegendItem(
            color = chartColors.warning,
            label = "Precaución (50-74%)",
            modifier = Modifier.weight(1f)
        )
        LegendItem(
            color = chartColors.critical,
            label = "Crítico (75-99%)",
            modifier = Modifier.weight(1f)
        )
        LegendItem(
            color = MaterialTheme.colorScheme.error,
            label = "Excedido (≥100%)",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = color,
                    size = size,
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawBudgetComparisonBar(
    budgetAmountCents: Long,
    spentAmountCents: Long,
    barColor: Color,
    backgroundColor: Color,
    errorColor: Color,
    canvasSize: Size
) {
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    
    // Draw background bar (full budget)
    drawRoundRect(
        color = backgroundColor,
        size = canvasSize,
        cornerRadius = cornerRadius
    )
    
    // Draw spent bar
    val spentPercentage = if (budgetAmountCents == 0L) 0f 
                         else (spentAmountCents.toFloat() / budgetAmountCents.toFloat()).coerceAtMost(1f)
    
    val spentWidth = canvasSize.width * spentPercentage
    
    if (spentWidth > 0) {
        drawRoundRect(
            color = barColor,
            size = Size(spentWidth, canvasSize.height),
            cornerRadius = cornerRadius
        )
    }
    
    // Draw overflow indicator if over budget
    if (spentAmountCents > budgetAmountCents) {
        val overflowWidth = 4.dp.toPx()
        drawRect(
            color = errorColor,
            topLeft = Offset(canvasSize.width - overflowWidth, 0f),
            size = Size(overflowWidth, canvasSize.height)
        )
    }
}