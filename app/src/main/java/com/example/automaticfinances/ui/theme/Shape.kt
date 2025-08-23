package com.example.automaticfinances.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ===========================================
// AutomaticFinances - Material 3 Shapes
// Shape system optimizado para finanzas
// ===========================================

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // Chips pequeños, badges
    small = RoundedCornerShape(8.dp),         // Botones pequeños, inputs
    medium = RoundedCornerShape(12.dp),       // Tarjetas estándar, cards
    large = RoundedCornerShape(16.dp),        // Modales, bottom sheets
    extraLarge = RoundedCornerShape(28.dp)    // FABs, elementos especiales
)

// === Shapes específicas para finanzas ===
object FinanceShapes {
    // Cards de transacciones
    val transactionCard = RoundedCornerShape(12.dp)
    
    // Cards de balance/saldo
    val balanceCard = RoundedCornerShape(16.dp)
    
    // Botones de acción principal
    val primaryButton = RoundedCornerShape(8.dp)
    
    // Chips de categorías
    val categoryChip = RoundedCornerShape(16.dp)
    
    // Bottom sheets y modales
    val bottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Dialog containers
    val dialog = RoundedCornerShape(24.dp)
    
    // Input fields
    val textField = RoundedCornerShape(8.dp)
    
    // Indicadores de estado
    val statusIndicator = RoundedCornerShape(4.dp)
    
    // Charts y gráficos contenedores
    val chartContainer = RoundedCornerShape(12.dp)
    
    // Progress indicators
    val progressIndicator = RoundedCornerShape(8.dp)
}