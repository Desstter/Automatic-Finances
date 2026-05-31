package com.example.automaticfinances.system

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Primary, restriction-proof path: ask the system to rebind the notification
                // listener. When it reconnects, onListenerConnected starts the foreground service
                // from a valid context. This works even on Android 12+ where starting a foreground
                // service directly from a boot broadcast can be blocked.
                runCatching {
                    NotificationListenerService.requestRebind(
                        ComponentName(context, SmsNotifListener::class.java)
                    )
                }

                // Secondary best-effort path: also try to start the service directly. Wrapped so a
                // background-start restriction can never crash the boot receiver.
                runCatching { ForegroundSmsService.startService(context) }
            }
        }
    }
}