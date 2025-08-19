package com.example.automaticfinances.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.automaticfinances.MainActivity
import com.example.automaticfinances.R
import kotlinx.coroutines.*

class ForegroundSmsService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_service_channel"
        private const val HEALTH_CHECK_INTERVAL = 30000L // 30 seconds
        
        fun startService(context: Context) {
            val intent = Intent(context, ForegroundSmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundSmsService::class.java)
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var healthCheckJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startHealthCheck()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Ensure notification listener is enabled
        ServiceManager.ensureNotificationListenerEnabled(this)
        
        return START_STICKY // Restart if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        healthCheckJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitorea SMS de Bancolombia"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutomaticFinances Activo")
            .setContentText("Monitoreando SMS de Bancolombia")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun startHealthCheck() {
        healthCheckJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Update notification with current status
                    val updatedNotification = createNotification()
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, updatedNotification)
                    
                    // Check if notification listener is still enabled
                    if (!ServiceManager.isNotificationListenerEnabled(this@ForegroundSmsService)) {
                        // Try to re-enable or restart
                        ServiceManager.ensureNotificationListenerEnabled(this@ForegroundSmsService)
                    }
                    
                } catch (e: Exception) {
                    // Silent fail, continue health check
                }
                
                delay(HEALTH_CHECK_INTERVAL)
            }
        }
    }
}