package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

@Composable
fun KPICard(
    title: String,
    currentValue: String,
    icon: String = "📊",
    subtitle: String? = null,
    trend: KPITrend? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (onClick != null) {
        modifier.fillMaxWidth()
    } else {
        modifier.fillMaxWidth()
    }
    
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
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
            
            // Main value
            Text(
                text = currentValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            // Subtitle if provided
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val percentageUsed = if (totalBudgetCents == 0L) 0f 
                        else (totalSpentCents.toFloat() / totalBudgetCents.toFloat()) * 100f
    
    val color = when {
        percentageUsed >= 100f -> MaterialTheme.colorScheme.error
        percentageUsed >= 75f -> Color(0xFFFF9800) // Orange
        percentageUsed >= 50f -> Color(0xFFFFC107) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
    
    val trend = if (overBudgetCount > 0) {
        KPITrend.DOWN
    } else if (percentageUsed < 50f) {
        KPITrend.UP
    } else {
        KPITrend.NEUTRAL
    }
    
    val subtitle = if (overBudgetCount > 0) {
        "$overBudgetCount presupuesto(s) excedido(s)"
    } else {
        "$budgetsCount presupuesto(s) activo(s)"
    }
    
    KPICard(
        title = "Presupuestos",
        currentValue = "${percentageUsed.toInt()}% usado",
        icon = "💰",
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
            else -> KPITrend.NEUTRAL
        }
    }
    
    val subtitle = previousMonthSpentCents?.let { previous ->
        val difference = monthlySpentCents - previous
        val percentageChange = if (previous == 0L) 0f 
                              else (difference.toFloat() / previous.toFloat()) * 100f
        val sign = if (difference >= 0) "+" else ""
        "$sign${nf.format(difference / 100.0)} (${sign}${percentageChange.toInt()}%)"
    }
    
    KPICard(
        title = "Gasto del Mes",
        currentValue = currentValue,
        icon = "💳",
        subtitle = subtitle,
        trend = trend,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TrendIndicator(trend: KPITrend) {
    val (icon, color) = when (trend) {
        KPITrend.UP -> "↗️" to Color(0xFF4CAF50)
        KPITrend.DOWN -> "↘️" to MaterialTheme.colorScheme.error
        KPITrend.NEUTRAL -> "➡️" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Text(
        text = icon,
        style = MaterialTheme.typography.titleMedium
    )
}

enum class KPITrend {
    UP, DOWN, NEUTRAL
}