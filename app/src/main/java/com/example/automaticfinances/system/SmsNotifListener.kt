package com.example.automaticfinances.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.example.automaticfinances.data.parse.BancolombiaParser
import com.example.automaticfinances.domain.AddTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsNotifListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val addTx = AddTransactionUseCase()

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Ensure foreground service is running when listener connects
        ForegroundSmsService.startService(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Try to restart the service if disconnected
        requestRebind(android.content.ComponentName(this, SmsNotifListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn?.notification ?: return
        val packageName = sbn.packageName
        val extras = n.extras
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "") +
                   " " +
                   (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "") +
                   " " +
                   (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "")

        // Filtrar por apps bancarias conocidas o contenido relevante
        if (shouldProcessNotification(packageName, text)) {
            scope.launch { 
                try {
                    val tx = BancolombiaParser.tryParse(text) ?: return@launch
                    addTx(tx) 
                } catch (e: Exception) {
                    // Silent fail to avoid crashes
                }
            }
        }
    }

    private fun shouldProcessNotification(packageName: String, text: String): Boolean {
        // Apps bancarias conocidas
        val bankApps = setOf(
            "com.bancolombia.androidapp",      // Bancolombia oficial
            "com.mobile.bancolombia",          // Bancolombia alternativo
            "com.nequi.MobileApp",             // Nequi
            "com.davivienda.daviplata",        // DaviPlata
            "co.com.ach.pse.app.avianca",      // Avianca LifeMiles
            "com.bbva.netcash",                // BBVA Colombia
            "com.bancodebogota.digital",       // Banco de Bogotá
            "android"                          // SMS system app
        )

        // Verificar si es de una app bancaria conocida
        if (bankApps.any { packageName.contains(it, ignoreCase = true) }) {
            return true
        }

        // Verificar por contenido (palabras clave bancarias)
        val bankKeywords = listOf(
            "bancolombia", "compraste", "transferiste", "transferencia",
            "nequi", "pagaste", "daviplata", "transacción", "débito",
            "compra", "retiro", "consignación", "pago"
        )

        return bankKeywords.any { text.contains(it, ignoreCase = true) }
    }
}