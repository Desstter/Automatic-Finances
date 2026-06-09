package com.example.automaticfinances.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic heartbeat that re-binds the notification listener. Aggressive OEMs (MIUI/HyperOS) silently
 * UNBIND third-party NotificationListenerServices while the app is idle, often without calling
 * onListenerDisconnected, so nothing self-heals until the user next opens the app. This worker closes
 * that gap for app-push banks (Nequi, DaviPlata, BBVA, Banco de Bogotá) that have no SMS fallback.
 *
 * [ServiceManager.requestListenerRebind] is a no-op when the listener is already bound or the
 * permission isn't granted, so running this on a schedule is always safe. On rebind,
 * [SmsNotifListener.onListenerConnected] replays any notifications still in the shade, so a push that
 * arrived during an unbound window is captured on the next heartbeat.
 */
class ListenerRebindWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Re-anchor the process first: if HyperOS killed the app since the last heartbeat, this brings
        // it back to foreground priority (and re-binds the listener as a side effect). Starting an FGS
        // here is permitted because the app is battery-optimization-exempt.
        runCatching { CaptureKeepAliveService.start(applicationContext) }
        ServiceManager.requestListenerRebind(applicationContext)
        runCatching { VoiceQuickActionNotifier.show(applicationContext) }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "listener_rebind_heartbeat"
    }
}
