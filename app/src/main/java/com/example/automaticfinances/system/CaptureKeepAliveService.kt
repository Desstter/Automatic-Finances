package com.example.automaticfinances.system

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Standalone process-anchor foreground service. This is the single most important piece of the
 * background-survival strategy on aggressive OEMs (Xiaomi HyperOS / MIUI).
 *
 * Why a dedicated service instead of promoting the [SmsNotifListener] itself: a
 * NotificationListenerService that calls startForeground() only stays foreground while it is *bound*.
 * HyperOS routinely unbinds third-party listeners, which tore down that foreground state and left the
 * process killable — a chicken-and-egg trap where the very mechanism meant to keep the process alive
 * died with the thing it was protecting. This service is independent of the listener binding: once
 * started it keeps the whole app process at foreground priority, which (a) keeps the listener bound so
 * app-push banks are captured, and (b) keeps a warm process so the [SmsReceiver] broadcast path runs
 * reliably for SMS banks.
 *
 * Foreground type is SPECIAL_USE on Android 14+ (no daily runtime cap), falling back to DATA_SYNC on
 * older releases. DATA_SYNC gained a ~6h/day cumulative timeout on Android 15 that would terminate an
 * always-on anchor — SPECIAL_USE has no such cap, which is exactly what an indefinite capture anchor
 * needs. The app being battery-optimization-exempt is what allows starting this from background
 * (boot / worker); keep that exemption.
 *
 * The foreground notification reuses [VoiceQuickActionNotifier]'s ongoing voice quick action (same
 * fixed id 1001), so the anchor and the voice shortcut are one and the same notification — no second
 * persistent entry in the shade.
 */
class CaptureKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        // START_STICKY: if the system kills us under memory pressure, ask it to recreate the service
        // (and thus re-anchor the process) as soon as resources allow.
        return START_STICKY
    }

    private fun startAsForeground() {
        runCatching {
            val notification = VoiceQuickActionNotifier.buildNotification(this)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> startForeground(
                    VoiceQuickActionNotifier.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> startForeground(
                    VoiceQuickActionNotifier.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
                else -> startForeground(VoiceQuickActionNotifier.NOTIFICATION_ID, notification)
            }
        }.onFailure { Log.e(TAG, "startForeground failed", it) }
    }

    companion object {
        private const val TAG = "CaptureKeepAlive"

        /**
         * Starts (or re-starts) the anchor. Idempotent: if it is already running, onStartCommand just
         * refreshes the foreground notification. Wrapped in runCatching because a background FGS start
         * can throw on Android 12+ if the app ever loses its battery exemption — we never want capture
         * setup to crash the caller (App / BootReceiver / Worker).
         */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CaptureKeepAliveService::class.java),
                )
            }.onFailure { Log.w(TAG, "Could not start keep-alive service", it) }
        }
    }
}
