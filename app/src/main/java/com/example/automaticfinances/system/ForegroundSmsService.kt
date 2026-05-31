package com.example.automaticfinances.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.automaticfinances.ui.voice.VoiceEntryActivity

class ForegroundSmsService : Service() {
    
    companion object {
        private const val TAG = "ForegroundSmsService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_service_channel"
        private const val REQUEST_VOICE_ENTRY = 2001

        /**
         * Best-effort service start. Wrapped so background-start restrictions on Android 12+
         * (e.g. when triggered from a boot broadcast) can never crash the caller — the
         * notification listener rebind path will bring the service back regardless.
         */
        fun startService(context: Context) {
            try {
                val intent = Intent(context, ForegroundSmsService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not start foreground service", e)
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
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            // Promotion to foreground can be denied (background-start restrictions). Don't crash;
            // stop quietly and let the listener rebind restart us from a valid context.
            Log.w(TAG, "startForeground denied; stopping", e)
            stopSelf()
        }

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
                "Registro por voz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Acceso rápido para registrar gastos hablando"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Tapping anywhere on the notification (or the explicit action) opens the translucent
        // voice-entry overlay — the voice flow is the primary, most direct purpose of this
        // persistent notification. NEW_TASK + CLEAR_TOP so it floats over whatever is on screen
        // and reuses the single-top instance if already open.
        val voiceIntent = Intent(this, VoiceEntryActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val voicePendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_VOICE_ENTRY,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Registrar un gasto")
            .setContentText("Toca para hablar y registrarlo al instante")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Mic — signals the voice shortcut
            .setContentIntent(voicePendingIntent)
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "Hablar",
                voicePendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}