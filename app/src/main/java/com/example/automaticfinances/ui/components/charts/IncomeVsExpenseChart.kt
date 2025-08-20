package com.example.automaticfinances.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.IncomeVsExpenseComparison
import java.text.NumberFormat
import java.util.*

@Composable
fun IncomeVsExpenseChart(
    comparison: IncomeVsExpenseComparison,
    modifier: Modifier = Modifier,
    numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
) {
    val animationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
    
    var animationPlayed by remember(comparison) { mutableStateOf(false) }
    val animatedIncomePercentage by animateFloatAsState(
        targetValue = if (animationPlayed) comparison.incomePercentage else 0f,
        animationSpec = animationSpec,
        label = "incomePercentage"
    )
    val animatedExpensePercentage by animateFloatAsState(
        targetValue = if (animationPlayed) comparison.expensePercentage else 0f,
        animationSpec = animationSpec,
        label = "expensePercentage"
    )

    LaunchedEffect(comparison) {
        animationPlayed = true
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                Text(
                    text = "💰 Ingresos vs Gastos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (comparison.hasPositiveBalance) "📈" else "📉",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Balance Neto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = numberFormat.format(comparison.netBalanceCents / 100.0),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (comparison.hasPositiveBalance) {
                            Color(0xFF4CAF50)
                        } else {
                            Color(0xFFF44336)
                        }
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tasa de Ahorro",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", 
                            if (comparison.totalIncomeCents > 0) 
                                (comparison.netBalanceCents.toFloat() / comparison.totalIncomeCents) * 100 
                            else 0f
                        )}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Bar Chart
            IncomeExpenseBar(
                incomeAmount = comparison.totalIncomeCents,
                expenseAmount = comparison.totalExpensesCents,
                incomePercentage = animatedIncomePercentage,
                expensePercentage = animatedExpensePercentage,
                numberFormat = numberFormat
            )
        }
    }
}

@Composable
private fun IncomeExpenseBar(
    incomeAmount: Long,
    expenseAmount: Long,
    incomePercentage: Float,
    expensePercentage: Float,
    numberFormat: NumberFormat
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Bar
        IncomeExpenseBarItem(
            label = "Ingresos",
            amount = incomeAmount,
            percentage = incomePercentage,
            color = Color(0xFF4CAF50),
            icon = "💰",
            numberFormat = numberFormat
        )
        
        // Expense Bar
        IncomeExpenseBarItem(
            label = "Gastos",
            amount = expenseAmount,
            percentage = expensePercentage,
            color = Color(0xFFF44336),
            icon = "💸",
            numberFormat = numberFormat
        )
    }
}

@Composable
private fun IncomeExpenseBarItem(
    label: String,
    amount: Long,
    percentage: Float,
    color: Color,
    icon: String,
    numberFormat: NumberFormat
) {
    Column {
        // Label and Amount
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
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = numberFormat.format(amount / 100.0),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        // Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}