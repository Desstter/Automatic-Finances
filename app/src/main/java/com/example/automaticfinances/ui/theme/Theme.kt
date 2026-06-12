package com.example.automaticfinances.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.automaticfinances.data.preferences.AccentColor

// ===========================================
// AutomaticFinances - Material 3 Theme ("Oro Refinado", estilo expresivo)
// Con soporte Dynamic Color y tokens financieros.
// NOTA: MaterialExpressiveTheme/MotionScheme son `internal` en material3 1.4.0
// (solo públicos en 1.5.0-alpha). El lenguaje expresivo se consigue aquí con la
// paleta nueva (Color.kt), formas más redondeadas (Shape.kt) y motion springy
// (Motion.kt) sobre el MaterialTheme estable.
// ===========================================

@Composable
fun FinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    accentColor: AccentColor = AccentColor.GOLD,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColorSchemeFallback
        else -> LightColorSchemeFallback
    }

    // El acento personal reemplaza los tokens primary. GOLD es no-op, así que el dynamic color
    // sigue mandando cuando el usuario no eligió un acento propio.
    val colorScheme = baseScheme.withAccent(accentColor, darkTheme)

    val financeColors = getFinanceColors(darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

// Acceso a tokens extra de finanzas
object FinanceTheme {
    val colors: FinanceColors
        @Composable get() = LocalFinanceColors.current
    
    val typography: FinanceTypography
        @Composable get() = FinanceTypography
    
    val shapes: FinanceShapes  
        @Composable get() = FinanceShapes
}

// === Legacy theme para compatibilidad ===

@Composable
fun AutomaticFinancesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but disabled by default
    // to maintain consistent branding across all Android versions
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FinanceTheme(
        darkTheme = darkTheme,
        useDynamicColor = dynamicColor,
        content = content
    )
}