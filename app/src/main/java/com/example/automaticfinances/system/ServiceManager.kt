package com.example.automaticfinances.system

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextUtils

object ServiceManager {

    /**
     * Asks the system to (re)bind the notification listener so detection goes live promptly after a
     * cold start, instead of waiting for the next incoming notification to trigger a bind. No-op if
     * the permission isn't granted; wrapped because rebind can throw on some OEMs. Cheap and safe to
     * call from any lifecycle hook (app start, activity resume, boot).
     */
    fun requestListenerRebind(context: Context) {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, SmsNotifListener::class.java),
            )
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = android.content.ComponentName.unflattenFromString(name)
                if (componentName != null && packageName == componentName.packageName) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Opens the system "Notification access" settings so the user can grant the listener
     * permission. An app CANNOT grant this permission to itself — this is the only legitimate
     * programmatic action. Must be triggered by a user action from a UI context; callers should
     * not invoke it from background loops (it would launch Settings repeatedly).
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * True if the app is exempt from battery optimizations (Doze). When false, aggressive OEMs are
     * free to kill the process and unbind the notification listener while idle, which is exactly how
     * bank notifications get dropped. The SMS receiver still works without this, but app-push banks
     * depend on the listener staying alive.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Shows the system dialog asking the user to exempt the app from battery optimizations. The
     * BatteryLife lint is suppressed deliberately: reliable background bank-notification capture is
     * the app's core function, which is a legitimate use of the direct-request intent. No-op if
     * already exempt. Must be triggered by a user action from a UI context.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}