package com.example.automaticfinances.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.automaticfinances.MainActivity
import com.example.automaticfinances.R
import com.example.automaticfinances.ui.voice.VoiceEntryActivity

class ForegroundSmsService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_service_channel"
        private const val REQUEST_VOICE_ENTRY = 2001

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
    
    override fun onCreate() {
        super.onCreate()
        ServiceManager.setServiceRunning(true)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY // Restart if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.setServiceRunning(false)
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

        // Tapping this action opens the translucent voice-entry overlay. NEW_TASK + CLEAR_TOP so it
        // floats over whatever is on screen and reuses the single-top instance if already open.
        val voiceIntent = Intent(this, VoiceEntryActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val voicePendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_VOICE_ENTRY,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutomaticFinances Activo")
            .setContentText("Monitoreando SMS de Bancolombia")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "Registrar gasto por voz",
                voicePendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}