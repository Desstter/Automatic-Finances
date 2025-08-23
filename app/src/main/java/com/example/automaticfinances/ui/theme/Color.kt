package com.example.automaticfinances.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ===========================================
// AutomaticFinances - Material 3 Complete Theme
// Professional color scheme for banking/finance
// ===========================================

/** Seed para generar la identidad visual cuando no hay Dynamic Color */
private val FinanceSeed = Color(0xFF4CAF50) // Verde sobrio para finanzas

// === LIGHT FALLBACK (Material 3 roles completos) ===
val LightColorSchemeFallback: ColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8E6B8),
    onPrimaryContainer = Color(0xFF1B5E20),

    secondary = Color(0xFF2196F3),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),

    tertiary = Color(0xFFFF9800),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFCC80),
    onTertiaryContainer = Color(0xFFE65100),

    error = Color(0xFFE53935),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),

    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF7F2FA),
    surfaceContainerHigh = Color(0xFFF1ECF4),
    surfaceContainerHighest = Color(0xFFECE6F0),
    surfaceContainerLow = Color(0xFFFDF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFFFFBFE),
    surfaceDim = Color(0xFFE0D9E1),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF322F37),
    inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFF81C784),

    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF4CAF50),
)

// === DARK FALLBACK ===
val DarkColorSchemeFallback: ColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFB8E6B8),

    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1565C0),
    onSecondaryContainer = Color(0xFFBBDEFB),

    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFFE65100),
    tertiaryContainer = Color(0xFFF57C00),
    onTertiaryContainer = Color(0xFFFFCC80),

    error = Color(0xFFFF5449),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceBright = Color(0xFF3A383E),
    surfaceDim = Color(0xFF141218),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F37),
    inversePrimary = Color(0xFF4CAF50),

    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF81C784),
)

// === Tokens extra para finanzas ===
@Stable
data class FinanceColors(
    val profit: Color,
    val onProfit: Color,
    val profitContainer: Color,
    val onProfitContainer: Color,
    val loss: Color,
    val onLoss: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

private val LightFinanceColors = FinanceColors(
    profit = Color(0xFF2E7D32),
    onProfit = Color(0xFFFFFFFF),
    profitContainer = Color(0xFFB8E6B8),
    onProfitContainer = Color(0xFF1B5E20),
    loss = Color(0xFFD32F2F),
    onLoss = Color(0xFFFFFFFF),
    lossContainer = Color(0xFFFFEBEE),
    onLossContainer = Color(0xFFB71C1C),
    warning = Color(0xFFF57C00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFCC80),
    onWarningContainer = Color(0xFFE65100),
    info = Color(0xFF1565C0),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFBBDEFB),
    onInfoContainer = Color(0xFF0D47A1),
)

private val DarkFinanceColors = FinanceColors(
    profit = Color(0xFF81C784),
    onProfit = Color(0xFF1B5E20),
    profitContainer = Color(0xFF2E7D32),
    onProfitContainer = Color(0xFFB8E6B8),
    loss = Color(0xFFFF5449),
    onLoss = Color(0xFFB71C1C),
    lossContainer = Color(0xFFD32F2F),
    onLossContainer = Color(0xFFFFEBEE),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFFE65100),
    warningContainer = Color(0xFFF57C00),
    onWarningContainer = Color(0xFFFFCC80),
    info = Color(0xFF64B5F6),
    onInfo = Color(0xFF0D47A1),
    infoContainer = Color(0xFF1565C0),
    onInfoContainer = Color(0xFFBBDEFB),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

// Función para seleccionar los colores financieros apropiados
internal fun getFinanceColors(darkTheme: Boolean): FinanceColors {
    return if (darkTheme) DarkFinanceColors else LightFinanceColors
}

// === Legacy colors for compatibility ===

// Primary Colors - Green (Financial Success & Growth)
val Green80 = Color(0xFFB8E6B8)      // Light green for dark theme
val Green60 = Color(0xFF81C784)      // Medium green 
val Green40 = Color(0xFF4CAF50)      // Main brand green (Material Green)
val Green20 = Color(0xFF2E7D32)      // Dark green for light theme

// Secondary Colors - Blue (Trust & Security)
val Blue80 = Color(0xFFBBDEFB)       // Light blue for dark theme
val Blue60 = Color(0xFF64B5F6)       // Medium blue
val Blue40 = Color(0xFF2196F3)       // Main blue (Material Blue)
val Blue20 = Color(0xFF1565C0)       // Dark blue for light theme

// Accent Colors - Orange (Attention & Warnings)
val Orange80 = Color(0xFFFFCC80)     // Light orange for dark theme
val Orange60 = Color(0xFFFFB74D)     // Medium orange
val Orange40 = Color(0xFFFF9800)     // Main orange (Material Orange)
val Orange20 = Color(0xFFF57C00)     // Dark orange for light theme

// Status Colors
val Success = Color(0xFF4CAF50)      // Green for success states
val Warning = Color(0xFFFF9800)      // Orange for warnings  
val Error = Color(0xFFE53935)        // Red for errors
val Info = Color(0xFF2196F3)         // Blue for info

// Income/Expense Colors
val IncomeGreen = Color(0xFF4CAF50)   // Positive transactions
val ExpenseRed = Color(0xFFE53935)    // Negative transactions
val TransferBlue = Color(0xFF2196F3)  // Transfers

// Neutral Colors
val Grey90 = Color(0xFF212121)        // Very dark grey
val Grey80 = Color(0xFF424242)        // Dark grey
val Grey60 = Color(0xFF757575)        // Medium grey
val Grey40 = Color(0xFFBDBDBD)        // Light grey
val Grey20 = Color(0xFFE0E0E0)        // Very light grey
val Grey10 = Color(0xFFF5F5F5)        // Almost white

// Material 3 Neutral Colors (Official Tones)
val Neutral99 = Color(0xFFFFFBFE)          // Casi blanco para fondos claros
val Neutral95 = Color(0xFFF7F2FA)          // Superficie muy clara
val Neutral90 = Color(0xFFE7E0EC)          // Superficie clara variante
val Neutral80 = Color(0xFFCAC4D0)          // Contornos claros
val Neutral70 = Color(0xFFAEA9B1)          // Texto secundario claro
val Neutral60 = Color(0xFF938F99)          // Contornos medios
val Neutral50 = Color(0xFF79747E)          // Texto terciario
val Neutral40 = Color(0xFF605D64)          // Contornos oscuros
val Neutral30 = Color(0xFF49454F)          // Superficie oscura variante
val Neutral20 = Color(0xFF322F37)          // Superficie oscura
val Neutral10 = Color(0xFF1D1B20)          // Texto en superficie clara
val Neutral0 = Color(0xFF000000)           // Negro absoluto (solo para casos especiales)

// Background Colors (Material 3 Compliant)
val BackgroundLight = Neutral99            // Light theme background
val BackgroundDark = Color(0xFF141218)     // Dark theme background (ligeramente más claro que negro)
val SurfaceLight = Neutral99               // Light theme surface
val SurfaceDark = Color(0xFF141218)        // Dark theme surface

// Legacy colors for compatibility
val Purple80 = Green80
val PurpleGrey80 = Grey40
val Pink80 = Orange80

val Purple40 = Green20
val PurpleGrey40 = Grey60
val Pink40 = Orange20