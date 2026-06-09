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
                // Re-anchor the process right after boot/update. Starting an FGS from BOOT_COMPLETED is
                // explicitly allowed; this brings the app back to foreground priority so HyperOS keeps
                // it alive and the listener rebinds. This is the recovery path after a reboot, when
                // both the process and the listener binding are gone.
                runCatching { CaptureKeepAliveService.start(context) }

                // Ask the system to rebind the notification listener after a reboot/update. Once it
                // reconnects, onListenerConnected marks detection live again.
                runCatching {
                    NotificationListenerService.requestRebind(
                        ComponentName(context, SmsNotifListener::class.java)
                    )
                }
                // Re-post the persistent voice quick-action notification: a reboot/update clears
                // ongoing notifications, so it must be re-issued to stay available.
                runCatching { VoiceQuickActionNotifier.show(context) }
            }
        }
    }
}