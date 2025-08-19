package com.example.automaticfinances.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.system.ServiceManager
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stateFlow: StateFlow<HomeState>,
    onOpenNotifAccess: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onManageCategoriesClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {}
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val nf = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }
    
    // Check service status
    val isServiceRunning by remember {
        derivedStateOf { ServiceManager.isServiceRunning(context) }
    }
    val isListenerEnabled by remember {
        derivedStateOf { ServiceManager.isNotificationListenerEnabled(context) }
    }
    
    Scaffold(
        topBar = { 
            TopAppBar(title = { Text("AutomaticFinances") }) 
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = onAddTransactionClick,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("💵")
                }
                FloatingActionButton(
                    onClick = onManageCategoriesClick,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("🏷️")
                }
                ExtendedFloatingActionButton(
                    onClick = onOpenNotifAccess
                ) {
                    Text("Habilitar acceso")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Service status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning && isListenerEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isServiceRunning && isListenerEnabled) "🟢" else "🔴",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isServiceRunning && isListenerEnabled) {
                                "Servicio Activo"
                            } else {
                                "Servicio Inactivo"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when {
                                !isListenerEnabled -> "Habilitar acceso a notificaciones"
                                !isServiceRunning -> "Iniciando servicio..."
                                else -> "Monitoreando SMS de Bancolombia"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Total monthly spending card con botón de historial
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onViewHistoryClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total del mes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            nf.format(state.totalMonthCOP / 100.0), 
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text("📊", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Ver historial",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn {
                items(state.transactions) { tx ->
                    TransactionItem(
                        transaction = tx,
                        numberFormat = nf,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = numberFormat.format(transaction.amountCents / 100.0),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            supportingContent = {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Icono de categoría
                    transaction.categoryIcon?.let { icon ->
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    // Nombre de categoría
                    transaction.categoryName?.let { categoryName ->
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = transaction.categoryColor?.let { 
                                Color(android.graphics.Color.parseColor(it))
                            } ?: MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    // Fecha y hora
                    Text(
                        text = "${transaction.date} ${transaction.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Indicador de origen (automático vs manual)
                    Text(
                        text = when {
                            transaction.source?.startsWith("notif") == true -> "🤖"
                            transaction.source == "manual" -> "✋"
                            else -> "📱"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }
}