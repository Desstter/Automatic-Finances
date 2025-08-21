package com.example.automaticfinances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceOverviewCard(
    bankBalanceCents: Long,
    cashBalanceCents: Long,
    totalBalanceCents: Long,
    monthlyIncome: Long,
    monthlyExpenses: Long,
    numberFormat: NumberFormat,
    onBankClick: () -> Unit = {},
    onCashClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBalances by remember { mutableStateOf(true) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with total balance and visibility toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (showBalances) {
                            numberFormat.format(totalBalanceCents / 100.0)
                        } else {
                            "• • • • •"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalBalanceCents >= 0) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                IconButton(
                    onClick = { showBalances = !showBalances },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = if (showBalances) "👁️" else "🙈",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bank and Cash balances row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bank Balance
                AccountBalanceItem(
                    title = "Banco",
                    balance = bankBalanceCents,
                    icon = "🏦",
                    color = Color(0xFF2196F3),
                    showBalance = showBalances,
                    numberFormat = numberFormat,
                    onClick = onBankClick,
                    modifier = Modifier.weight(1f)
                )
                
                // Cash Balance
                AccountBalanceItem(
                    title = "Efectivo",
                    balance = cashBalanceCents,
                    icon = "💵",
                    color = Color(0xFF4CAF50),
                    showBalance = showBalances,
                    numberFormat = numberFormat,
                    onClick = onCashClick,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Monthly income vs expenses
            MonthlyBreakdownSection(
                monthlyIncome = monthlyIncome,
                monthlyExpenses = monthlyExpenses,
                showBalances = showBalances,
                numberFormat = numberFormat,
                onViewHistoryClick = onViewHistoryClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountBalanceItem(
    title: String,
    balance: Long,
    icon: String,
    color: Color,
    showBalance: Boolean,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (showBalance) {
                    numberFormat.format(balance / 100.0)
                } else {
                    "• • •"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (balance >= 0) color else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MonthlyBreakdownSection(
    monthlyIncome: Long,
    monthlyExpenses: Long,
    showBalances: Boolean,
    numberFormat: NumberFormat,
    onViewHistoryClick: () -> Unit
) {
    val monthlyBalance = monthlyIncome - monthlyExpenses
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Este Mes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(onClick = onViewHistoryClick) {
                Text("Ver detalles")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Income
            MonthlyItem(
                label = "Ingresos",
                amount = monthlyIncome,
                icon = "💰",
                color = Color(0xFF4CAF50),
                showBalance = showBalances,
                numberFormat = numberFormat,
                modifier = Modifier.weight(1f)
            )
            
            // Expenses
            MonthlyItem(
                label = "Gastos",
                amount = monthlyExpenses,
                icon = "💸",
                color = Color(0xFFFF5722),
                showBalance = showBalances,
                numberFormat = numberFormat,
                modifier = Modifier.weight(1f)
            )
            
            // Balance
            MonthlyItem(
                label = "Balance",
                amount = monthlyBalance,
                icon = if (monthlyBalance >= 0) "📈" else "📉",
                color = if (monthlyBalance >= 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
                showBalance = showBalances,
                numberFormat = numberFormat,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthlyItem(
    label: String,
    amount: Long,
    icon: String,
    color: Color,
    showBalance: Boolean,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = if (showBalance) {
                numberFormat.format(amount / 100.0)
            } else {
                "•••"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}