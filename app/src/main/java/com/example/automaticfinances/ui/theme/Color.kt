package com.example.automaticfinances.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ===========================================
// AutomaticFinances — "Peso de Oro" (Bogota Modernista)
// Colombian Gold accent palette
// ===========================================

// === Colombian Gold — Primary brand identity ===
val BogotaGold      = Color(0xFFD4A853)
val BogotaGoldDark  = Color(0xFFA07828)
val BogotaGoldLight = Color(0xFFE8C87A)

// === Warm dark backgrounds ===
val DeepCharcoal    = Color(0xFF1A1714)   // dark background
val WarmCharcoal    = Color(0xFF231F1B)   // dark surface
val RichCharcoal    = Color(0xFF2C2722)   // dark surfaceContainer
val CharcoalBorder  = Color(0xFF3D3731)   // dark outlineVariant

// === Warm light backgrounds ===
val WarmCream       = Color(0xFFFAF7F2)   // light background
val ParchmentCream  = Color(0xFFF2EDE5)   // light surface
val LightParchment  = Color(0xFFEDE7DC)   // light surfaceContainer

// === Financial semantic ===
val EmeraldProfit      = Color(0xFF2D8B4E)   // Colombian emerald green
val EmeraldProfitLight = Color(0xFF4CAF50)
val CrimsonLoss        = Color(0xFFC0392B)
val AmberWarning       = Color(0xFFC97B2A)

// === Material 3 Complete Dark Color Scheme ===
val DarkColorSchemeFallback: ColorScheme = darkColorScheme(
    primary              = BogotaGold,
    onPrimary            = Color(0xFF1A1714),
    primaryContainer     = Color(0xFF3D3020),
    onPrimaryContainer   = BogotaGoldLight,
    secondary            = Color(0xFF6FB3D3),
    onSecondary          = Color(0xFF0D2A3A),
    secondaryContainer   = Color(0xFF1E3D4F),
    onSecondaryContainer = Color(0xFFB8D9ED),
    tertiary             = Color(0xFF9B8EA8),
    onTertiary           = Color(0xFF1C1525),
    tertiaryContainer    = Color(0xFF332944),
    onTertiaryContainer  = Color(0xFFD8CCDF),
    error                = Color(0xFFEF5350),
    onError              = Color(0xFF1A0000),
    errorContainer       = Color(0xFF3D1F1F),
    onErrorContainer     = Color(0xFFFFCDD2),
    background           = DeepCharcoal,
    onBackground         = Color(0xFFF5F0E8),
    surface              = WarmCharcoal,
    onSurface            = Color(0xFFF5F0E8),
    surfaceVariant       = Color(0xFF3D3731),
    onSurfaceVariant     = Color(0xFF9E9086),
    surfaceContainer     = RichCharcoal,
    surfaceContainerHigh = Color(0xFF353028),
    surfaceContainerHighest = Color(0xFF3E382F),
    surfaceContainerLow  = Color(0xFF26221E),
    surfaceContainerLowest = DeepCharcoal,
    surfaceBright        = Color(0xFF4A4440),
    surfaceDim           = Color(0xFF1A1714),
    outline              = Color(0xFF6E6560),
    outlineVariant       = CharcoalBorder,
    inverseSurface       = WarmCream,
    inverseOnSurface     = Color(0xFF2C2420),
    inversePrimary       = BogotaGoldDark,
    scrim                = Color(0xFF000000),
    surfaceTint          = BogotaGold,
)

// === Material 3 Complete Light Color Scheme ===
val LightColorSchemeFallback: ColorScheme = lightColorScheme(
    primary              = BogotaGoldDark,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFF5E8C8),
    onPrimaryContainer   = Color(0xFF4A3200),
    secondary            = Color(0xFF2E86AB),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCEE8F5),
    onSecondaryContainer = Color(0xFF0A2D40),
    tertiary             = Color(0xFF7B6D8E),
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFEBDEF7),
    onTertiaryContainer  = Color(0xFF2B1D40),
    error                = CrimsonLoss,
    onError              = Color.White,
    errorContainer       = Color(0xFFFCE8E6),
    onErrorContainer     = Color(0xFF7A1210),
    background           = WarmCream,
    onBackground         = Color(0xFF2C2420),
    surface              = ParchmentCream,
    onSurface            = Color(0xFF2C2420),
    surfaceVariant       = Color(0xFFE8E0D5),
    onSurfaceVariant     = Color(0xFF7A6E67),
    surfaceContainer     = LightParchment,
    surfaceContainerHigh = Color(0xFFE5DDD2),
    surfaceContainerHighest = Color(0xFFDDD4C8),
    surfaceContainerLow  = Color(0xFFF5F0E8),
    surfaceContainerLowest = WarmCream,
    surfaceBright        = ParchmentCream,
    surfaceDim           = Color(0xFFDDD5C8),
    outline              = Color(0xFFA89E96),
    outlineVariant       = Color(0xFFDDD5C8),
    inverseSurface       = Color(0xFF2C2420),
    inverseOnSurface     = WarmCream,
    inversePrimary       = BogotaGold,
    scrim                = Color(0xFF000000),
    surfaceTint          = BogotaGoldDark,
)

// === Finance-specific semantic tokens ===
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
    profit              = EmeraldProfit,
    onProfit            = Color.White,
    profitContainer     = Color(0xFFB8E6C4),
    onProfitContainer   = Color(0xFF0D3D20),
    loss                = CrimsonLoss,
    onLoss              = Color.White,
    lossContainer       = Color(0xFFFCE8E6),
    onLossContainer     = Color(0xFF7A1210),
    warning             = AmberWarning,
    onWarning           = Color.White,
    warningContainer    = Color(0xFFF5E0C0),
    onWarningContainer  = Color(0xFF5A3200),
    info                = Color(0xFF2E86AB),
    onInfo              = Color.White,
    infoContainer       = Color(0xFFCEE8F5),
    onInfoContainer     = Color(0xFF0A2D40),
)

private val DarkFinanceColors = FinanceColors(
    profit              = EmeraldProfitLight,
    onProfit            = Color(0xFF0D3D20),
    profitContainer     = Color(0xFF1A5C30),
    onProfitContainer   = Color(0xFFB8E6C4),
    loss                = Color(0xFFEF7070),
    onLoss              = Color(0xFF5A0000),
    lossContainer       = Color(0xFF3D1010),
    onLossContainer     = Color(0xFFFFCDD2),
    warning             = Color(0xFFE8B060),
    onWarning           = Color(0xFF3D2000),
    warningContainer    = Color(0xFF5A3800),
    onWarningContainer  = Color(0xFFF5DFB0),
    info                = Color(0xFF6FB3D3),
    onInfo              = Color(0xFF0A2D40),
    infoContainer       = Color(0xFF1E3D4F),
    onInfoContainer     = Color(0xFFB8D9ED),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

internal fun getFinanceColors(darkTheme: Boolean): FinanceColors =
    if (darkTheme) DarkFinanceColors else LightFinanceColors

// === Legacy aliases for compatibility ===
val Green80 = BogotaGoldLight
val Green60 = BogotaGold
val Green40 = BogotaGoldDark
val Green20 = Color(0xFF6B4F10)

val Blue80  = Color(0xFFB8D9ED)
val Blue60  = Color(0xFF6FB3D3)
val Blue40  = Color(0xFF2E86AB)
val Blue20  = Color(0xFF0A2D40)

val Orange80 = Color(0xFFF5DFB0)
val Orange60 = Color(0xFFE8B060)
val Orange40 = AmberWarning
val Orange20 = Color(0xFF5A3200)

val Success  = EmeraldProfitLight
val Warning  = AmberWarning
val Error    = CrimsonLoss
val Info     = Color(0xFF2E86AB)

val IncomeGreen  = EmeraldProfit
val ExpenseRed   = CrimsonLoss
val TransferBlue = Color(0xFF2E86AB)

val Grey90 = Color(0xFF212121)
val Grey80 = Color(0xFF424242)
val Grey60 = Color(0xFF757575)
val Grey40 = Color(0xFFBDBDBD)
val Grey20 = Color(0xFFE0E0E0)
val Grey10 = Color(0xFFF5F5F5)

val Neutral99  = WarmCream
val Neutral95  = Color(0xFFF5F0E8)
val Neutral90  = LightParchment
val Neutral80  = Color(0xFFDDD5C8)
val Neutral70  = Color(0xFFBFB5AB)
val Neutral60  = Color(0xFFA89E96)
val Neutral50  = Color(0xFF8E8077)
val Neutral40  = Color(0xFF7A6E67)
val Neutral30  = CharcoalBorder
val Neutral20  = RichCharcoal
val Neutral10  = WarmCharcoal
val Neutral0   = Color(0xFF000000)

val BackgroundLight = WarmCream
val BackgroundDark  = DeepCharcoal
val SurfaceLight    = ParchmentCream
val SurfaceDark     = WarmCharcoal

val Purple80     = BogotaGoldLight
val PurpleGrey80 = Grey40
val Pink80       = Orange80
val Purple40     = BogotaGoldDark
val PurpleGrey40 = Grey60
val Pink40       = Orange20
