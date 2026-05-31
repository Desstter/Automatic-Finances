package com.example.automaticfinances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.ui.theme.FinanceTheme
import java.text.NumberFormat

@Composable
fun AccountSelectorCard(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selecciona la cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            accounts.forEach { account ->
                AccountSelectionItem(
                    account = account,
                    isSelected = account.id == selectedAccountId,
                    onAccountSelected = { onAccountSelected(account.id) },
                    numberFormat = numberFormat
                )
                
                if (account.id != accounts.last().id) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectionItem(
    account: Account,
    isSelected: Boolean,
    onAccountSelected: () -> Unit,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val accountIcon: ImageVector
    val accountColor: Color
    when (account.type) {
        AccountType.BANK -> {
            accountIcon = Icons.Default.AccountBalance
            accountColor = FinanceTheme.colors.info
        }
        AccountType.CASH -> {
            accountIcon = Icons.Default.Payments
            accountColor = FinanceTheme.colors.profit
        }
    }
    
    Card(
        onClick = onAccountSelected,
        modifier = modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = accountColor.copy(alpha = 0.1f)
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, accountColor)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radio button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) accountColor else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .background(
                        color = if (isSelected) accountColor else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Dot sits on the arbitrary account color; pick contrast by luminance.
                    val onAccount = if (accountColor.luminance() > 0.5f) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(onAccount, CircleShape)
                    )
                }
            }
            
            // Account icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accountColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = accountIcon,
                    contentDescription = null,
                    tint = accountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Account info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) accountColor else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Balance actual: ${numberFormat.format(account.balanceCents / 100.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (account.balanceCents >= 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionada",
                    tint = accountColor
                )
            }
        }
    }
}

@Composable
fun AccountBalancePreview(
    account: Account?,
    incomeAmount: Long,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    if (account == null || incomeAmount <= 0) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FinanceTheme.colors.profitContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Se agregará a: ${account.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = numberFormat.format(account.balanceCents / 100.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = null,
                        tint = FinanceTheme.colors.profit,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = numberFormat.format((account.balanceCents + incomeAmount) / 100.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceTheme.colors.profit,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "+${numberFormat.format(incomeAmount / 100.0)}",
                style = MaterialTheme.typography.titleMedium,
                color = FinanceTheme.colors.profit,
                fontWeight = FontWeight.Bold
            )
        }
    }
}