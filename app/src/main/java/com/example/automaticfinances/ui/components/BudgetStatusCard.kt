package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.BudgetStatus
import com.example.automaticfinances.data.db.BudgetAlertLevel
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetStatusCard(
    budgetStatus: BudgetStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    val alertColor = when (budgetStatus.alertLevel) {
        BudgetAlertLevel.SAFE -> Color(0xFF4CAF50)
        BudgetAlertLevel.WARNING -> Color(0xFFFFC107)
        BudgetAlertLevel.CRITICAL -> Color(0xFFFF9800)
        BudgetAlertLevel.OVER_BUDGET -> MaterialTheme.colorScheme.error
    }
    
    val alertText = when (budgetStatus.alertLevel) {
        BudgetAlertLevel.SAFE -> "En buen estado"
        BudgetAlertLevel.WARNING -> "Precaución"
        BudgetAlertLevel.CRITICAL -> "Crítico"
        BudgetAlertLevel.OVER_BUDGET -> "Excedido"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with category info and alert level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = budgetStatus.category.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = budgetStatus.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Surface(
                    color = alertColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = alertText,
                        style = MaterialTheme.typography.labelSmall,
                        color = alertColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = nf.format(budgetStatus.currentSpentCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = nf.format(budgetStatus.budget.limitAmountCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    // Background bar
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                    
                    // Progress bar
                    val progressWidth = (budgetStatus.percentageUsed / 100f).coerceIn(0f, 1f)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(progressWidth)
                            .fillMaxHeight(),
                        color = alertColor
                    ) {}
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${budgetStatus.percentageUsed.toInt()}% usado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Additional info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Restante",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nf.format(budgetStatus.remainingCents / 100.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (budgetStatus.remainingCents > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                
                if (budgetStatus.daysLeftInMonth > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Días restantes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${budgetStatus.daysLeftInMonth}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Projection warning if over budget projected
            if (budgetStatus.projectedSpentCents > budgetStatus.budget.limitAmountCents && budgetStatus.daysLeftInMonth > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚠️", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Proyección: ${nf.format(budgetStatus.projectedSpentCents / 100.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetStatusList(
    budgetStatuses: List<BudgetStatus>,
    onBudgetClick: (BudgetStatus) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        budgetStatuses.forEach { budgetStatus ->
            BudgetStatusCard(
                budgetStatus = budgetStatus,
                onClick = { onBudgetClick(budgetStatus) }
            )
        }
    }
}