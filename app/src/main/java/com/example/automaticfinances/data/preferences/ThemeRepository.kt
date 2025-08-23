package com.example.automaticfinances.data.preferences

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ===========================================
// Theme Management System
// Siguiendo Material Design 3 guidelines con Dynamic Color
// ===========================================

enum class ThemeMode {
    AUTO,    // Sigue el tema del sistema
    LIGHT,   // Tema claro forzado
    DARK     // Tema oscuro forzado
}

// DataStore extension for Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemeRepository(private val context: Context) {
    
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("use_dynamic_color")
    }
    
    // Flow para observar cambios en el tema
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE_KEY] ?: ThemeMode.AUTO.name
        try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.AUTO // Fallback seguro
        }
    }
    
    // Flow para observar cambios en colores dinámicos
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        // Por defecto true en Android 12+, false en versiones anteriores
        preferences[DYNAMIC_COLOR_KEY] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
    
    // Cambiar el modo de tema
    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }
    
    // Cambiar configuración de colores dinámicos
    suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = useDynamicColor
        }
    }
    
    // Métodos de conveniencia para tema
    suspend fun setLightTheme() = setThemeMode(ThemeMode.LIGHT)
    suspend fun setDarkTheme() = setThemeMode(ThemeMode.DARK)
    suspend fun setAutoTheme() = setThemeMode(ThemeMode.AUTO)
    
    // Toggle entre Light y Dark (manteniendo AUTO si está activo)
    suspend fun toggleTheme(currentMode: ThemeMode) {
        val nextMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.AUTO -> ThemeMode.LIGHT // Si está en auto, cambiar a manual light
        }
        setThemeMode(nextMode)
    }
    
    // Toggle colores dinámicos
    suspend fun toggleDynamicColor(currentUseDynamicColor: Boolean) {
        setUseDynamicColor(!currentUseDynamicColor)
    }
}