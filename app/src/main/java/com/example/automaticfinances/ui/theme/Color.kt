package com.example.automaticfinances.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ===========================================
// AutomaticFinances — "Oro Refinado"
// Gold brand accent over clean warm neutrals.
// Full Material 3 tonal system (light + dark), generated from seeds:
//   primary  = Bogota Gold   (hue ~75)
//   secondary= Emerald       (hue ~150, ties to the `profit` semantic)
//   tertiary = Petroleo/Teal (hue ~210)
// ===========================================

// === Gold — primary brand identity ===
val BogotaGold      = Color(0xFFF2BE48)   // dark primary / inversePrimary (tone ~80)
val BogotaGoldDark  = Color(0xFF7E5A00)   // light primary / surfaceTint   (tone ~40)
val BogotaGoldLight = Color(0xFFFFDEA0)   // primary container highlight   (tone ~90)

// === Warm dark backgrounds (low-chroma warm neutral) ===
val DeepCharcoal    = Color(0xFF16130F)   // dark background
val WarmCharcoal    = Color(0xFF1F1B16)   // dark surface
val RichCharcoal    = Color(0xFF231F19)   // dark surfaceContainer
val CharcoalBorder  = Color(0xFF4D4639)   // dark outlineVariant

// === Warm light backgrounds (cleaner than cream) ===
val WarmCream       = Color(0xFFFCF9F4)   // light background
val ParchmentCream  = Color(0xFFF6F1E9)   // light surface
val LightParchment  = Color(0xFFF2ECE2)   // light surfaceContainer

// === Financial semantic seeds ===
val EmeraldProfit      = Color(0xFF1F7A43)   // light profit
val EmeraldProfitLight = Color(0xFF6FD99A)   // dark profit
val CrimsonLoss        = Color(0xFFBA1A1A)
val AmberWarning       = Color(0xFFB25E00)

// === Material 3 Complete Dark Color Scheme ===
val DarkColorSchemeFallback: ColorScheme = darkColorScheme(
    primary              = BogotaGold,
    onPrimary            = Color(0xFF422D00),
    primaryContainer     = Color(0xFF5F4400),
    onPrimaryContainer   = BogotaGoldLight,
    secondary            = Color(0xFF9DD4AC),
    onSecondary          = Color(0xFF07391E),
    secondaryContainer   = Color(0xFF1D5135),
    onSecondaryContainer = Color(0xFFB8F0C6),
    tertiary             = Color(0xFF8DD3E4),
    onTertiary           = Color(0xFF00363F),
    tertiaryContainer    = Color(0xFF0B4F5B),
    onTertiaryContainer  = Color(0xFFAEEDFF),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = DeepCharcoal,
    onBackground         = Color(0xFFE9E1D6),
    surface              = WarmCharcoal,
    onSurface            = Color(0xFFE9E1D6),
    surfaceVariant       = Color(0xFF4D4639),
    onSurfaceVariant     = Color(0xFFD0C5B4),
    surfaceContainer     = RichCharcoal,
    surfaceContainerHigh = Color(0xFF2E2923),
    surfaceContainerHighest = Color(0xFF39342D),
    surfaceContainerLow  = Color(0xFF1F1B16),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceBright        = Color(0xFF453F36),
    surfaceDim           = Color(0xFF16130F),
    outline              = Color(0xFF998F80),
    outlineVariant       = CharcoalBorder,
    inverseSurface       = Color(0xFFE9E1D6),
    inverseOnSurface     = Color(0xFF33302A),
    inversePrimary       = BogotaGoldDark,
    scrim                = Color(0xFF000000),
    surfaceTint          = BogotaGold,
)

// === Material 3 Complete Light Color Scheme ===
val LightColorSchemeFallback: ColorScheme = lightColorScheme(
    primary              = BogotaGoldDark,
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = BogotaGoldLight,
    onPrimaryContainer   = Color(0xFF5E4300),
    secondary            = Color(0xFF356A45),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFB8F0C6),
    onSecondaryContainer = Color(0xFF19512F),
    tertiary             = Color(0xFF2A6A78),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFAEEDFF),
    onTertiaryContainer  = Color(0xFF00424E),
    error                = CrimsonLoss,
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    background           = WarmCream,
    onBackground         = Color(0xFF1E1B16),
    surface              = ParchmentCream,
    onSurface            = Color(0xFF1E1B16),
    surfaceVariant       = Color(0xFFEBE2D2),
    onSurfaceVariant     = Color(0xFF4D4639),
    surfaceContainer     = LightParchment,
    surfaceContainerHigh = Color(0xFFECE6DC),
    surfaceContainerHighest = Color(0xFFE6E0D6),
    surfaceContainerLow  = Color(0xFFF8F3EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceBright        = Color(0xFFFCF9F4),
    surfaceDim           = Color(0xFFDFD9CE),
    outline              = Color(0xFF837568),
    outlineVariant       = Color(0xFFD5C9B8),
    inverseSurface       = Color(0xFF33302A),
    inverseOnSurface     = Color(0xFFF7F0E7),
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
    onProfit            = Color(0xFFFFFFFF),
    profitContainer     = Color(0xFFB8F0C6),
    onProfitContainer   = Color(0xFF07391E),
    loss                = CrimsonLoss,
    onLoss              = Color(0xFFFFFFFF),
    lossContainer       = Color(0xFFFFDAD6),
    onLossContainer     = Color(0xFF410002),
    warning             = AmberWarning,
    onWarning           = Color(0xFFFFFFFF),
    warningContainer    = Color(0xFFFFDCBE),
    onWarningContainer  = Color(0xFF5A2E00),
    info                = Color(0xFF2A6A78),
    onInfo              = Color(0xFFFFFFFF),
    infoContainer       = Color(0xFFAEEDFF),
    onInfoContainer     = Color(0xFF00424E),
)

private val DarkFinanceColors = FinanceColors(
    profit              = EmeraldProfitLight,
    onProfit            = Color(0xFF07391E),
    profitContainer     = Color(0xFF1D5135),
    onProfitContainer   = Color(0xFFB8F0C6),
    loss                = Color(0xFFFFB4AB),
    onLoss              = Color(0xFF690005),
    lossContainer       = Color(0xFF93000A),
    onLossContainer     = Color(0xFFFFDAD6),
    warning             = Color(0xFFFFB877),
    onWarning           = Color(0xFF4A2800),
    warningContainer    = Color(0xFF6B3C00),
    onWarningContainer  = Color(0xFFFFDCBE),
    info                = Color(0xFF8DD3E4),
    onInfo              = Color(0xFF00363F),
    infoContainer       = Color(0xFF0B4F5B),
    onInfoContainer     = Color(0xFFAEEDFF),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

internal fun getFinanceColors(darkTheme: Boolean): FinanceColors =
    if (darkTheme) DarkFinanceColors else LightFinanceColors

// === Legacy aliases for compatibility ===
val Green80 = BogotaGoldLight
val Green60 = BogotaGold
val Green40 = BogotaGoldDark
val Green20 = Color(0xFF5E4300)

val Blue80  = Color(0xFFAEEDFF)
val Blue60  = Color(0xFF8DD3E4)
val Blue40  = Color(0xFF2A6A78)
val Blue20  = Color(0xFF00424E)

val Orange80 = Color(0xFFFFDCBE)
val Orange60 = Color(0xFFFFB877)
val Orange40 = AmberWarning
val Orange20 = Color(0xFF5A2E00)

val Success  = EmeraldProfit
val Warning  = AmberWarning
val Error    = CrimsonLoss
val Info     = Color(0xFF2A6A78)

val IncomeGreen  = EmeraldProfit
val ExpenseRed   = CrimsonLoss
val TransferBlue = Color(0xFF2A6A78)

val Grey90 = Color(0xFF212121)
val Grey80 = Color(0xFF424242)
val Grey60 = Color(0xFF757575)
val Grey40 = Color(0xFFBDBDBD)
val Grey20 = Color(0xFFE0E0E0)
val Grey10 = Color(0xFFF5F5F5)

val Neutral99  = WarmCream
val Neutral95  = Color(0xFFF8F3EB)
val Neutral90  = LightParchment
val Neutral80  = Color(0xFFE6E0D6)
val Neutral70  = Color(0xFFD5C9B8)
val Neutral60  = Color(0xFF837568)
val Neutral50  = Color(0xFF6F6356)
val Neutral40  = Color(0xFF4D4639)
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
