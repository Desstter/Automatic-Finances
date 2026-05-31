package com.example.automaticfinances.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ===========================================
// AutomaticFinances - Material 3 Shapes (estilo Expressive)
// Escala más redondeada/generosa para un look expresivo, manteniendo
// la legibilidad financiera. (M3 Expressive favorece radios mayores.)
// ===========================================

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // Chips pequeños, badges
    small = RoundedCornerShape(12.dp),        // Botones pequeños, inputs
    medium = RoundedCornerShape(20.dp),       // Tarjetas estándar, cards
    large = RoundedCornerShape(28.dp),        // Modales, contenedores grandes
    extraLarge = RoundedCornerShape(36.dp)    // FABs, elementos especiales
)

// === Shapes específicas para finanzas ===
object FinanceShapes {
    // Cards de transacciones
    val transactionCard = RoundedCornerShape(20.dp)

    // Cards de balance/saldo
    val balanceCard = RoundedCornerShape(28.dp)

    // Botones de acción principal (full/pill para look expresivo)
    val primaryButton = RoundedCornerShape(percent = 50)

    // Chips de categorías
    val categoryChip = RoundedCornerShape(percent = 50)
    
    // Bottom sheets y modales
    val bottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Dialog containers
    val dialog = RoundedCornerShape(28.dp)

    // Input fields
    val textField = RoundedCornerShape(12.dp)

    // Indicadores de estado
    val statusIndicator = RoundedCornerShape(percent = 50)

    // Charts y gráficos contenedores
    val chartContainer = RoundedCornerShape(20.dp)

    // Progress indicators (pill)
    val progressIndicator = RoundedCornerShape(percent = 50)
}