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

// ===========================================
// AutomaticFinances - Material 3 Complete Theme
// Con soporte Dynamic Color y tokens financieros
// ===========================================

@Composable
fun FinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColorSchemeFallback
        else -> LightColorSchemeFallback
    }

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