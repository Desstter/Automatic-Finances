package com.example.automaticfinances.system

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import com.example.automaticfinances.system.ServiceManager

/**
 * System configuration checker for AutomaticFinances
 * Monitors critical system states and determines when service status should be visible
 */
object SystemConfigurationChecker {
    
    /**
     * Comprehensive system health check
     */
    fun getSystemHealth(context: Context): SystemHealthStatus {
        val isServiceRunning = ServiceManager.isServiceRunning(context)
        val isListenerEnabled = ServiceManager.isNotificationListenerEnabled(context)
        val isBatteryOptimized = isBatteryOptimizationEnabled(context)
        
        return SystemHealthStatus(
            isServiceRunning = isServiceRunning,
            isListenerEnabled = isListenerEnabled,
            isBatteryOptimized = isBatteryOptimized,
            hasConfigurationIssues = !isServiceRunning || !isListenerEnabled || isBatteryOptimized,
            needsUserAttention = !isServiceRunning || !isListenerEnabled
        )
    }
    
    /**
     * Determines if service status should be visible on HomeScreen
     * Only shows when there are issues or during grace period after fixes
     */
    fun shouldShowServiceStatus(context: Context, lastFixTimestamp: Long = 0L): Boolean {
        val systemHealth = getSystemHealth(context)
        
        // Always show if there are issues
        if (systemHealth.needsUserAttention) {
            return true
        }
        
        // Show for grace period after user fixes issues (5 minutes)
        val gracePeriodMs = 5 * 60 * 1000L // 5 minutes
        val currentTime = System.currentTimeMillis()
        if (lastFixTimestamp > 0 && (currentTime - lastFixTimestamp) < gracePeriodMs) {
            return true
        }
        
        // Hide when everything is working correctly
        return false
    }
    
    /**
     * Check if battery optimization is enabled for this app
     */
    private fun isBatteryOptimizationEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                return !isIgnoring
            } else {
                // For API < 23, battery optimization doesn't exist
                return false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get user-friendly configuration messages
     */
    fun getConfigurationMessage(context: Context): ConfigurationMessage {
        val health = getSystemHealth(context)
        
        return when {
            !health.isListenerEnabled -> ConfigurationMessage(
                title = "Permisos Requeridos",
                message = "Activa el acceso a notificaciones para detectar SMS automáticamente",
                icon = "⚠️",
                severity = MessageSeverity.CRITICAL,
                actionText = "Activar Permisos"
            )
            
            !health.isServiceRunning -> ConfigurationMessage(
                title = "Servicio Detenido",
                message = "El servicio de monitoreo SMS no está activo",
                icon = "🔄",
                severity = MessageSeverity.WARNING,
                actionText = "Reiniciar Servicio"
            )
            
            health.isBatteryOptimized -> ConfigurationMessage(
                title = "Optimización de Batería",
                message = "Desactiva la optimización para funcionamiento 24/7",
                icon = "🔋",
                severity = MessageSeverity.INFO,
                actionText = "Configurar"
            )
            
            else -> ConfigurationMessage(
                title = "Sistema Activo",
                message = "AutomaticFinances está funcionando correctamente",
                icon = "✅",
                severity = MessageSeverity.SUCCESS,
                actionText = null
            )
        }
    }
    
    /**
     * Composable for reactive system health monitoring
     */
    @Composable
    fun rememberSystemHealth(context: Context): State<SystemHealthStatus> {
        return produceState(
            initialValue = getSystemHealth(context),
            context
        ) {
            while (true) {
                value = getSystemHealth(context)
                delay(30_000) // Check every 30 seconds
            }
        }
    }
}

/**
 * Data class representing system health status
 */
data class SystemHealthStatus(
    val isServiceRunning: Boolean,
    val isListenerEnabled: Boolean,
    val isBatteryOptimized: Boolean,
    val hasConfigurationIssues: Boolean,
    val needsUserAttention: Boolean
)

/**
 * Configuration message for user feedback
 */
data class ConfigurationMessage(
    val title: String,
    val message: String,
    val icon: String,
    val severity: MessageSeverity,
    val actionText: String?
)

/**
 * Message severity levels
 */
enum class MessageSeverity {
    SUCCESS,
    INFO,
    WARNING,
    CRITICAL
}