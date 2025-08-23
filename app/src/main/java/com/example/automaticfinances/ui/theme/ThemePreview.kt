package com.example.automaticfinances.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ===========================================
// AutomaticFinances - Theme Demo & Previews
// Componentes de demostración y settings
// ===========================================

@Composable
fun ThemeDemo() {
    val c = MaterialTheme.colorScheme
    val f = FinanceTheme.colors
    val typography = MaterialTheme.typography
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header con balance principal
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = FinanceTheme.shapes.balanceCard
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saldo actual",
                        style = typography.titleMedium,
                        color = c.onSurfaceVariant
                    )
                    Text(
                        text = "$2,456,789",
                        style = FinanceTypography.moneyLarge,
                        color = c.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = FinanceTheme.shapes.primaryButton
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar gasto")
                        }
                        
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = FinanceTheme.shapes.primaryButton
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Análisis")
                        }
                    }
                }
            }
        }
        
        item {
            // Sección de ingresos vs gastos
            Text(
                text = "Resumen del mes",
                style = typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(getSampleTransactions()) { transaction ->
            TransactionDemoCard(transaction = transaction)
        }
        
        item {
            // Color tokens demo
            Text(
                text = "Tokens de color financieros",
                style = typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            ColorTokensDemo()
        }
        
        item {
            // Typography demo
            Text(
                text = "Tipografía financiera",
                style = typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            TypographyDemo()
        }
    }
}

@Composable
fun TransactionDemoCard(transaction: SampleTransaction) {
    val f = FinanceTheme.colors
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinanceTheme.shapes.transactionCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (transaction.type) {
                            TransactionType.INCOME -> f.profitContainer
                            TransactionType.EXPENSE -> f.lossContainer
                            TransactionType.TRANSFER -> f.infoContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transaction.icon,
                    contentDescription = null,
                    tint = when (transaction.type) {
                        TransactionType.INCOME -> f.onProfitContainer
                        TransactionType.EXPENSE -> f.onLossContainer
                        TransactionType.TRANSFER -> f.onInfoContainer
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = transaction.category,
                    style = FinanceTypography.category,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.date,
                    style = FinanceTypography.dateTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = when (transaction.type) {
                    TransactionType.INCOME -> "+${transaction.amount}"
                    TransactionType.EXPENSE -> "-${transaction.amount}"
                    TransactionType.TRANSFER -> transaction.amount
                },
                style = FinanceTypography.moneyMedium,
                color = when (transaction.type) {
                    TransactionType.INCOME -> f.profit
                    TransactionType.EXPENSE -> f.loss
                    TransactionType.TRANSFER -> f.info
                }
            )
        }
    }
}

@Composable
fun ColorTokensDemo() {
    val f = FinanceTheme.colors
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinanceTheme.shapes.chartContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorTokenRow(
                label = "Ganancias",
                primaryColor = f.profit,
                containerColor = f.profitContainer,
                amount = "+$150,000"
            )
            ColorTokenRow(
                label = "Pérdidas",
                primaryColor = f.loss,
                containerColor = f.lossContainer,
                amount = "-$75,000"
            )
            ColorTokenRow(
                label = "Advertencias",
                primaryColor = f.warning,
                containerColor = f.warningContainer,
                amount = "Límite: 80%"
            )
            ColorTokenRow(
                label = "Información",
                primaryColor = f.info,
                containerColor = f.infoContainer,
                amount = "3 pendientes"
            )
        }
    }
}

@Composable
fun ColorTokenRow(
    label: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Surface(
            color = containerColor,
            shape = FinanceTheme.shapes.statusIndicator,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = amount,
                style = FinanceTypography.moneySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TypographyDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinanceTheme.shapes.chartContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Money Large", style = FinanceTypography.moneyLarge)
            Text("Money Medium", style = FinanceTypography.moneyMedium)
            Text("Money Small", style = FinanceTypography.moneySmall)
            Text("Account: **** 1234", style = FinanceTypography.accountNumber)
            Text("12 Dic 2024, 14:30", style = FinanceTypography.dateTime)
            Text("CATEGORÍA", style = FinanceTypography.category)
            Text("COMPLETADO", style = FinanceTypography.transactionStatus)
        }
    }
}

@Composable
fun SettingsThemeScreen(
    darkTheme: Boolean,
    useDynamicColor: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de tema",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Switch tema oscuro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tema oscuro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Usar tema oscuro en lugar del tema claro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = onDarkThemeChange
                    )
                }
                
                HorizontalDivider()
                
                // Switch colores dinámicos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Colores dinámicos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Usar colores del wallpaper del sistema (Android 12+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }
        }
        
        // Información adicional
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "Sobre los colores dinámicos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Los colores dinámicos extraen una paleta de tu wallpaper para crear una experiencia personalizada. Esta función está disponible en Android 12 y versiones posteriores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// === Data classes para demo ===

data class SampleTransaction(
    val description: String,
    val amount: String,
    val category: String,
    val date: String,
    val type: TransactionType,
    val icon: ImageVector
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

fun getSampleTransactions(): List<SampleTransaction> = listOf(
    SampleTransaction(
        description = "Sueldo diciembre",
        amount = "$3,500,000",
        category = "Ingresos",
        date = "01 Dic 2024",
        type = TransactionType.INCOME,
        icon = Icons.Default.Star
    ),
    SampleTransaction(
        description = "Compra supermercado",
        amount = "$156,000",
        category = "Alimentación",
        date = "15 Dic 2024",
        type = TransactionType.EXPENSE,
        icon = Icons.Default.ShoppingCart
    ),
    SampleTransaction(
        description = "Pago Netflix",
        amount = "$45,000",
        category = "Entretenimiento",
        date = "14 Dic 2024",
        type = TransactionType.EXPENSE,
        icon = Icons.Default.Favorite
    ),
    SampleTransaction(
        description = "Transferencia a Juan",
        amount = "$200,000",
        category = "Transferencias",
        date = "13 Dic 2024",
        type = TransactionType.TRANSFER,
        icon = Icons.Default.Home
    ),
    SampleTransaction(
        description = "Pago tarjeta crédito",
        amount = "$890,000",
        category = "Pagos",
        date = "12 Dic 2024",
        type = TransactionType.EXPENSE,
        icon = Icons.Default.Settings
    )
)

// === Previews ===

@Preview(name = "Theme Demo Light", showBackground = true)
@Composable
private fun ThemeDemoLightPreview() {
    FinanceTheme(darkTheme = false, useDynamicColor = false) {
        ThemeDemo()
    }
}

@Preview(name = "Theme Demo Dark", showBackground = true)
@Composable
private fun ThemeDemoDarkPreview() {
    FinanceTheme(darkTheme = true, useDynamicColor = false) {
        ThemeDemo()
    }
}

@Preview(name = "Settings Light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    FinanceTheme(darkTheme = false, useDynamicColor = false) {
        SettingsThemeScreen(
            darkTheme = false,
            useDynamicColor = true,
            onDarkThemeChange = {},
            onDynamicColorChange = {}
        )
    }
}

@Preview(name = "Settings Dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    FinanceTheme(darkTheme = true, useDynamicColor = false) {
        SettingsThemeScreen(
            darkTheme = true,
            useDynamicColor = false,
            onDarkThemeChange = {},
            onDynamicColorChange = {}
        )
    }
}

@Preview(name = "Transaction Card", showBackground = true)
@Composable
private fun TransactionCardPreview() {
    FinanceTheme(darkTheme = false, useDynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                getSampleTransactions().take(3).forEach { transaction ->
                    TransactionDemoCard(transaction = transaction)
                }
            }
        }
    }
}