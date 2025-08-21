package com.example.automaticfinances.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
    
    // Progress-based color system
    val progressPercentage = budgetStatus.percentageUsed
    val progressColor = when {
        progressPercentage <= 50f -> MaterialTheme.colorScheme.primary
        progressPercentage <= 75f -> MaterialTheme.colorScheme.tertiary
        progressPercentage <= 90f -> MaterialTheme.colorScheme.secondary
        progressPercentage < 100f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.error
    }
    
    // Motivational Spanish messages based on progress
    val motivationalMessage = when {
        progressPercentage <= 25f -> "Acabas de empezar este mes"
        progressPercentage <= 50f -> "Vas por buen camino"
        progressPercentage <= 75f -> "Estás muy cerca del límite"
        progressPercentage <= 90f -> "¡Cuidado! Muy cerca del presupuesto"
        progressPercentage < 100f -> "¡Casi en el límite!"
        else -> "Presupuesto excedido"
    }
    
    // Message color and icon
    val messageColor = when {
        progressPercentage <= 50f -> MaterialTheme.colorScheme.primary
        progressPercentage <= 75f -> MaterialTheme.colorScheme.tertiary
        progressPercentage <= 90f -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.error
    }
    
    val messageIcon = when {
        progressPercentage <= 25f -> "🌱"
        progressPercentage <= 50f -> "✅"
        progressPercentage <= 75f -> "📊"
        progressPercentage <= 90f -> "⚠️"
        progressPercentage < 100f -> "🚨"
        else -> "❌"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = messageIcon,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = messageColor
                    )
                }
            }
            
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
                    
                    // Animated progress bar
                    val targetProgress = (budgetStatus.percentageUsed / 100f).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "progress_animation"
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight(),
                        color = progressColor
                    ) {}
                }
                
                // Motivational message
                Surface(
                    color = messageColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = messageIcon,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = motivationalMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = messageColor
                        )
                    }
                }
            }
            
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