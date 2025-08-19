package com.example.automaticfinances.system

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object ServiceManager {
    
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
    
    fun ensureNotificationListenerEnabled(context: Context) {
        if (!isNotificationListenerEnabled(context)) {
            // If not enabled, there's not much we can do programmatically
            // The user must enable it manually in settings
            return
        }
    }
    
    fun startPersistentService(context: Context) {
        ForegroundSmsService.startService(context)
    }
    
    fun stopPersistentService(context: Context) {
        ForegroundSmsService.stopService(context)
    }
    
    fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundSmsService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}