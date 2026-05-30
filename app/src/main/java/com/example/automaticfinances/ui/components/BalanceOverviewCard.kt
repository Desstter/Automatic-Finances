package com.example.automaticfinances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automaticfinances.ui.theme.FinanceTheme
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
    onViewBalancesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBalances by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showBalances) {
                            numberFormat.format(totalBalanceCents / 100.0)
                        } else {
                            "• • • • •"
                        },
                        style = com.example.automaticfinances.ui.theme.FinanceTypography.moneyLarge.copy(fontSize = 30.sp),
                        color = if (totalBalanceCents >= 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                IconButton(
                    onClick = { showBalances = !showBalances },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = if (showBalances) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showBalances) "Ocultar saldos" else "Mostrar saldos",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountBalanceItem(
                    title = "Banco",
                    balance = bankBalanceCents,
                    icon = Icons.Default.AccountBalance,
                    showBalance = showBalances,
                    numberFormat = numberFormat,
                    onClick = onBankClick,
                    modifier = Modifier.weight(1f)
                )
                AccountBalanceItem(
                    title = "Efectivo",
                    balance = cashBalanceCents,
                    icon = Icons.Default.Payments,
                    showBalance = showBalances,
                    numberFormat = numberFormat,
                    onClick = onCashClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
    icon: ImageVector,
    showBalance: Boolean,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsConfiguration = balance == 0L
    val contentColor = if (needsConfiguration) MaterialTheme.colorScheme.outline
                       else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (needsConfiguration)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (needsConfiguration) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (needsConfiguration) Icons.Default.Settings else icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (needsConfiguration) MaterialTheme.colorScheme.outline
                           else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (needsConfiguration) "Configurar $title" else title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (showBalance) numberFormat.format(balance / 100.0) else "• • •",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (needsConfiguration) MaterialTheme.colorScheme.outline
                        else if (balance >= 0) FinanceTheme.colors.profit
                        else MaterialTheme.colorScheme.error
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
            MonthlyItem(
                label = "Ingresos",
                amount = monthlyIncome,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = FinanceTheme.colors.profit,
                showBalance = showBalances,
                numberFormat = numberFormat,
                modifier = Modifier.weight(1f)
            )
            MonthlyItem(
                label = "Gastos",
                amount = monthlyExpenses,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                color = FinanceTheme.colors.loss,
                showBalance = showBalances,
                numberFormat = numberFormat,
                modifier = Modifier.weight(1f)
            )
            MonthlyItem(
                label = "Balance",
                amount = monthlyBalance,
                icon = if (monthlyBalance >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                color = if (monthlyBalance >= 0) FinanceTheme.colors.profit else FinanceTheme.colors.loss,
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
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    showBalance: Boolean,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (showBalance) numberFormat.format(amount / 100.0) else "•••",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
