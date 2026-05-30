package com.example.automaticfinances.ui.openingbalance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningBalanceSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    isFirstTime: Boolean = false,
    modifier: Modifier = Modifier
) {
    val viewModel: OpeningBalanceSetupViewModel = hiltViewModel()
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }
    
    // Handle setup completion
    LaunchedEffect(state.isSetupComplete) {
        if (state.isSetupComplete) {
            onSetupComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isFirstTime) "Configuración Inicial" else "Balances Iniciales",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isFirstTime) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header explanation
            item {
                if (isFirstTime) {
                    WelcomeHeader()
                } else {
                    UpdateHeader()
                }
            }
            
            // Date selector
            item {
                DateSelectorCard(
                    selectedDate = state.effectiveDate,
                    onDateChanged = viewModel::updateEffectiveDate
                )
            }
            
            // Account balance inputs
            items(state.accounts) { account ->
                AccountBalanceCard(
                    account = account,
                    balance = state.accountBalances[account.id] ?: 0.0,
                    onBalanceChanged = { newBalance ->
                        viewModel.updateAccountBalance(account.id, newBalance)
                    },
                    isLoading = state.isLoading
                )
            }
            
            // Warning card
            if (!isFirstTime) {
                item {
                    WarningCard()
                }
            }
            
            // Error display
            state.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Action buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = if (isFirstTime) Arrangement.End else Arrangement.spacedBy(12.dp)
                ) {
                    if (!isFirstTime) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.saveOpeningBalances() },
                        enabled = !state.isLoading && state.hasValidBalances,
                        modifier = if (isFirstTime) Modifier else Modifier.weight(1f)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isFirstTime) "Comenzar" else "Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "¡Bienvenido! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Para comenzar a usar AutomaticFinances, necesitamos conocer tus balances actuales:",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BulletPoint("¿Cuánto dinero tienes en el banco?")
                BulletPoint("¿Cuánto efectivo manejas?")
                BulletPoint("¿Desde qué fecha quieres hacer seguimiento?")
            }
            
            Text(
                text = "Estos serán tus \"balances iniciales\" y todos los reportes se calcularán a partir de esta fecha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpdateHeader() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Actualizar Balances Iniciales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Modifica tus balances iniciales y la fecha efectiva. Esto recalculará todos tus reportes.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorCard(
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000L
    )
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fecha Efectiva",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Los reportes se calcularán desde esta fecha en adelante",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { millis ->
                millis?.let {
                    val localDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000L))
                    onDateChanged(localDate)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun AccountBalanceCard(
    account: Account,
    balance: Double,
    onBalanceChanged: (Double) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    var balanceText by remember(balance) { mutableStateOf(balance.toString()) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (account.type) {
                            AccountType.BANK -> "Cuenta bancaria"
                            AccountType.CASH -> "Efectivo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (balance > 0) {
                    Text(
                        text = nf.format(balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            OutlinedTextField(
                value = balanceText,
                onValueChange = { newValue ->
                    balanceText = newValue
                    // Try to parse and update
                    newValue.toDoubleOrNull()?.let { parsedValue ->
                        if (parsedValue >= 0) {
                            onBalanceChanged(parsedValue)
                        }
                    }
                },
                label = { Text("Balance inicial") },
                prefix = { Text("$") },
                suffix = { Text("COP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Ingresa el monto que tienes actualmente") }
            )
        }
    }
}

@Composable
private fun WarningCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warningContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = com.example.automaticfinances.ui.theme.FinanceTheme.colors.warning
            )
            Column {
                Text(
                    text = "Importante",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Cambiar los balances iniciales recalculará todos tus reportes financieros desde la nueva fecha efectiva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// Extension to add warning container color
val ColorScheme.warningContainer: androidx.compose.ui.graphics.Color
    @Composable
    get() = MaterialTheme.colorScheme.secondaryContainer