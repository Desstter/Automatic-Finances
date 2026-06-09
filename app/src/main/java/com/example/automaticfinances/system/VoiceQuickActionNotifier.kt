package com.example.automaticfinances.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.automaticfinances.ui.voice.VoiceEntryActivity

/**
 * Posts and maintains the persistent "registrar por voz" notification — an always-available quick
 * action that opens the voice-entry overlay so the user can dictate an expense without opening the
 * app.
 *
 * It is an ordinary *ongoing* notification, deliberately NOT backed by a foreground service: the
 * NotificationListenerService is kept alive by the system on its own (no companion FGS needed to
 * receive bank notifications), and an ongoing notification sidesteps the Android 12+ background
 * foreground-service-start restrictions that made the old [ForegroundSmsService] fail silently from
 * boot/broadcast contexts. Re-posting it is cheap and idempotent (fixed id), so every entry point
 * that could see it dropped — app start, resume, boot, package update — simply calls [show].
 */
object VoiceQuickActionNotifier {

    private const val TAG = "VoiceQuickAction"
    const val CHANNEL_ID = "voice_quick_action"
    const val NOTIFICATION_ID = 1001
    private const val REQUEST_VOICE_ENTRY = 2001

    /** Creates the low-importance channel on demand. Safe to call repeatedly. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Registro por voz",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Acceso rápido para registrar gastos hablando"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts (or refreshes) the persistent voice notification. No-op when notifications are disabled
     * for the app (POST_NOTIFICATIONS denied on Android 13+, or the channel/app muted) — the system
     * would drop it anyway. Idempotent: the fixed id updates the existing notification in place
     * instead of stacking duplicates.
     */
    fun show(context: Context) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled; skipping voice quick action")
            return
        }

        try {
            manager.notify(NOTIFICATION_ID, buildNotification(context))
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check above and this call — ignore quietly.
            Log.w(TAG, "Notify denied for voice quick action", e)
        }
    }

    /**
     * Builds the persistent voice-entry notification. Shared with [CaptureKeepAliveService] so the
     * process-anchor foreground service and the standalone quick action are the *same* ongoing
     * notification (fixed id [NOTIFICATION_ID]) — never two competing entries for the same purpose.
     * Tapping it (or its explicit action) opens the translucent voice-entry overlay.
     */
    fun buildNotification(context: Context): android.app.Notification {
        ensureChannel(context)
        // NEW_TASK + CLEAR_TOP so the overlay floats over whatever is on screen and reuses the
        // single-top instance if already open.
        val voiceIntent = Intent(context, VoiceEntryActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_VOICE_ENTRY,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Registrar un gasto por voz")
            .setContentText("Toca para hablar y registrarlo al instante")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pending)
            .addAction(android.R.drawable.ic_btn_speak_now, "Hablar", pending)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
