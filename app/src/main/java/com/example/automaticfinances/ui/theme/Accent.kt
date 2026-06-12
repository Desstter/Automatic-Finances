package com.example.automaticfinances.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.automaticfinances.data.preferences.AccentColor

// ===========================================
// Acentos personales
// Permiten que el usuario haga suya la app cambiando el color de marca (tokens `primary`).
// GOLD = "Oro Refinado" por defecto: no aplica override, así el dynamic color sigue funcionando.
// El resto reemplaza solo los tokens primary del esquema base (Color.kt), conservando fondos,
// superficies y los semánticos financieros (profit/loss) intactos.
// ===========================================

/** Tokens primary que definen un acento para un modo (claro u oscuro). */
private data class AccentTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
)

/** Color de muestra para el selector de Ajustes (representa el acento en cualquier modo). */
fun AccentColor.swatch(): Color = when (this) {
    AccentColor.GOLD -> Color(0xFFF2BE48)
    AccentColor.EMERALD -> Color(0xFF2E9E68)
    AccentColor.OCEAN -> Color(0xFF3B82C4)
    AccentColor.VIOLET -> Color(0xFF8B5CF6)
    AccentColor.CORAL -> Color(0xFFE56A54)
}

/** Etiqueta legible para el selector de Ajustes. */
fun AccentColor.label(): String = when (this) {
    AccentColor.GOLD -> "Oro"
    AccentColor.EMERALD -> "Esmeralda"
    AccentColor.OCEAN -> "Océano"
    AccentColor.VIOLET -> "Violeta"
    AccentColor.CORAL -> "Coral"
}

private fun lightTokens(accent: AccentColor): AccentTokens? = when (accent) {
    AccentColor.GOLD -> null // usa el esquema base / dynamic color
    AccentColor.EMERALD -> AccentTokens(
        primary = Color(0xFF1F7A47),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA8F2C2),
        onPrimaryContainer = Color(0xFF00210F),
        inversePrimary = Color(0xFF6FD99A),
    )
    AccentColor.OCEAN -> AccentTokens(
        primary = Color(0xFF1F5FA6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E3FF),
        onPrimaryContainer = Color(0xFF001B3C),
        inversePrimary = Color(0xFFA4C8FF),
    )
    AccentColor.VIOLET -> AccentTokens(
        primary = Color(0xFF6C40C7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE9DDFF),
        onPrimaryContainer = Color(0xFF22005D),
        inversePrimary = Color(0xFFCFBCFF),
    )
    AccentColor.CORAL -> AccentTokens(
        primary = Color(0xFFB5341F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD3),
        onPrimaryContainer = Color(0xFF3F0400),
        inversePrimary = Color(0xFFFFB4A4),
    )
}

private fun darkTokens(accent: AccentColor): AccentTokens? = when (accent) {
    AccentColor.GOLD -> null
    AccentColor.EMERALD -> AccentTokens(
        primary = Color(0xFF6FD99A),
        onPrimary = Color(0xFF00391C),
        primaryContainer = Color(0xFF00522C),
        onPrimaryContainer = Color(0xFFA8F2C2),
        inversePrimary = Color(0xFF1F7A47),
    )
    AccentColor.OCEAN -> AccentTokens(
        primary = Color(0xFFA4C8FF),
        onPrimary = Color(0xFF00305F),
        primaryContainer = Color(0xFF004785),
        onPrimaryContainer = Color(0xFFD3E3FF),
        inversePrimary = Color(0xFF1F5FA6),
    )
    AccentColor.VIOLET -> AccentTokens(
        primary = Color(0xFFCFBCFF),
        onPrimary = Color(0xFF391E74),
        primaryContainer = Color(0xFF52389D),
        onPrimaryContainer = Color(0xFFE9DDFF),
        inversePrimary = Color(0xFF6C40C7),
    )
    AccentColor.CORAL -> AccentTokens(
        primary = Color(0xFFFFB4A4),
        onPrimary = Color(0xFF640D00),
        primaryContainer = Color(0xFF8C2812),
        onPrimaryContainer = Color(0xFFFFDAD3),
        inversePrimary = Color(0xFFB5341F),
    )
}

/**
 * Aplica el acento elegido sobre un esquema base. Para [AccentColor.GOLD] devuelve el esquema
 * sin cambios (marca por defecto / dynamic color). Para el resto reemplaza los tokens primary y
 * el surfaceTint, dejando fondos y semánticos financieros intactos.
 */
internal fun ColorScheme.withAccent(accent: AccentColor, darkTheme: Boolean): ColorScheme {
    val tokens = (if (darkTheme) darkTokens(accent) else lightTokens(accent)) ?: return this
    return copy(
        primary = tokens.primary,
        onPrimary = tokens.onPrimary,
        primaryContainer = tokens.primaryContainer,
        onPrimaryContainer = tokens.onPrimaryContainer,
        inversePrimary = tokens.inversePrimary,
        surfaceTint = tokens.primary,
    )
}
