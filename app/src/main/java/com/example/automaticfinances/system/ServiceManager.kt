package com.example.automaticfinances.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object ServiceManager {

    @Volatile
    private var serviceRunning: Boolean = false

    fun setServiceRunning(running: Boolean) {
        serviceRunning = running
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

    fun startPersistentService(context: Context) {
        ForegroundSmsService.startService(context)
    }
    
    fun stopPersistentService(context: Context) {
        ForegroundSmsService.stopService(context)
    }
    
    fun isServiceRunning(context: Context): Boolean = serviceRunning
}