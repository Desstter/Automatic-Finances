package com.example.automaticfinances.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.preferences.AccentColor
import com.example.automaticfinances.data.preferences.ThemeMode
import com.example.automaticfinances.data.preferences.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ===========================================
// Theme ViewModel
// Gestiona el estado del tema y colores dinámicos
// ===========================================

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {
    
    // Estado actual del modo de tema
    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ThemeMode.AUTO
        )
    
    // Estado actual de colores dinámicos
    val useDynamicColor: StateFlow<Boolean> = themeRepository.useDynamicColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )

    // Acento personal (color de marca elegido por el usuario)
    val accentColor: StateFlow<AccentColor> = themeRepository.accentColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AccentColor.GOLD
        )

    fun setAccentColor(accent: AccentColor) {
        viewModelScope.launch {
            themeRepository.setAccentColor(accent)
        }
    }

    // Nombre del usuario para el saludo personalizado
    val userName: StateFlow<String> = themeRepository.userName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ""
        )

    fun setUserName(name: String) {
        viewModelScope.launch {
            themeRepository.setUserName(name)
        }
    }
    
    // Computed state que determina si debe usar tema oscuro
    @Composable
    fun isDarkTheme(): Boolean {
        val currentMode = themeMode.collectAsState().value
        return when (currentMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> isSystemInDarkTheme()
        }
    }
    
    // Obtener estado de colores dinámicos
    @Composable
    fun getUseDynamicColor(): Boolean {
        return useDynamicColor.collectAsState().value
    }

    // Obtener acento personal actual
    @Composable
    fun getAccentColor(): AccentColor {
        return accentColor.collectAsState().value
    }
    
    // Cambiar a modo específico
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }
    
    // Cambiar configuración de colores dinámicos
    fun setUseDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch {
            themeRepository.setUseDynamicColor(useDynamicColor)
        }
    }
    
    // Toggle entre Light y Dark
    fun toggleTheme() {
        viewModelScope.launch {
            val currentMode = themeMode.value
            themeRepository.toggleTheme(currentMode)
        }
    }
    
    // Toggle colores dinámicos
    fun toggleDynamicColor() {
        viewModelScope.launch {
            val currentUseDynamicColor = useDynamicColor.value
            themeRepository.toggleDynamicColor(currentUseDynamicColor)
        }
    }
    
    // Métodos de conveniencia para tema
    fun setLightTheme() = setThemeMode(ThemeMode.LIGHT)
    fun setDarkTheme() = setThemeMode(ThemeMode.DARK)
    fun setAutoTheme() = setThemeMode(ThemeMode.AUTO)
    
    // Obtener el icono apropiado para el estado actual
    fun getThemeIcon(): ThemeIcon {
        return when (themeMode.value) {
            ThemeMode.LIGHT -> ThemeIcon.LIGHT
            ThemeMode.DARK -> ThemeIcon.DARK
            ThemeMode.AUTO -> ThemeIcon.AUTO
        }
    }
    
    // Obtener descripción del modo actual
    fun getThemeDescription(): String {
        return when (themeMode.value) {
            ThemeMode.LIGHT -> "Tema claro"
            ThemeMode.DARK -> "Tema oscuro"
            ThemeMode.AUTO -> "Automático (sistema)"
        }
    }
    
    // Verificar si Dynamic Color está disponible
    fun isDynamicColorAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    // Obtener descripción de colores dinámicos
    fun getDynamicColorDescription(): String {
        return if (isDynamicColorAvailable()) {
            if (useDynamicColor.value) {
                "Activo - Usando colores del sistema"
            } else {
                "Inactivo - Usando colores de la app"
            }
        } else {
            "No disponible en esta versión de Android"
        }
    }
}

// Enum para iconos de tema
enum class ThemeIcon {
    LIGHT,    // sun/light_mode icon
    DARK,     // moon/dark_mode icon  
    AUTO      // auto_mode/brightness_auto icon
}

