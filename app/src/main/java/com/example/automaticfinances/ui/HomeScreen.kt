package com.example.automaticfinances.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.Transaction
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stateFlow: StateFlow<HomeState>,
    onOpenNotifAccess: () -> Unit
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    
    val nf = remember { 
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }
    
    Scaffold(
        topBar = { 
            TopAppBar(title = { Text("AutomaticFinances") }) 
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenNotifAccess
            ) {
                Text("Habilitar acceso")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total del mes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        nf.format(state.totalMonthCOP / 100.0), 
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn {
                items(state.items) { tx ->
                    ListItem(
                        headlineContent = { 
                            Text(tx.type + (tx.merchant?.let { " — $it" } ?: "")) 
                        },
                        supportingContent = { 
                            Text(tx.rawPreview) 
                        },
                        trailingContent = { 
                            Text(nf.format(tx.amountCents / 100.0)) 
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}