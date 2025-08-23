package com.example.automaticfinances.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.example.automaticfinances.data.parse.BancolombiaParser
import com.example.automaticfinances.domain.AddTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class SmsNotifListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val addTx = AddTransactionUseCase()
    
    // Error tracking for monitoring and debugging
    private val parseErrors = AtomicInteger(0)
    private val dbErrors = AtomicInteger(0)
    private val processedNotifications = AtomicInteger(0)
    
    companion object {
        private const val TAG = "SmsNotifListener"
        private const val MAX_TEXT_LENGTH = 1000 // Prevent excessive logging
    }

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
            processedNotifications.incrementAndGet()
            
            scope.launch { 
                try {
                    processNotificationSafely(packageName, text)
                } catch (e: Exception) {
                    // Log critical failures but don't crash the service
                    Log.e(TAG, "Critical failure processing notification from $packageName", e)
                    logErrorStats()
                }
            }
        }
    }
    
    private suspend fun processNotificationSafely(packageName: String, text: String) {
        try {
            // Attempt to parse the SMS text
            val transaction = BancolombiaParser.tryParse(text)
            
            if (transaction == null) {
                // This is normal - not all notifications are parseable transactions
                Log.d(TAG, "No transaction parsed from $packageName: ${text.take(50)}...")
                return
            }
            
            // Attempt to save the transaction
            try {
                addTx(transaction)
                Log.d(TAG, "Successfully processed ${transaction.type} transaction: ${transaction.description}")
            } catch (e: Exception) {
                // Database or business logic error
                dbErrors.incrementAndGet()
                Log.e(TAG, "Database error saving transaction from $packageName", e)
                Log.e(TAG, "Transaction details: ${transaction.type}, ${transaction.description}, ${transaction.amountCents}")
                
                // For critical business errors, we might want to retry or store for later
                handleDatabaseError(transaction, e)
            }
            
        } catch (e: Exception) {
            // Parser error
            parseErrors.incrementAndGet()
            val safeText = text.take(MAX_TEXT_LENGTH)
            Log.e(TAG, "Parser error for notification from $packageName", e)
            Log.e(TAG, "Problematic text: $safeText")
            
            // For parser errors, log for improvement but continue processing
            handleParserError(packageName, safeText, e)
        }
    }
    
    private fun handleDatabaseError(transaction: com.example.automaticfinances.data.db.Transaction, error: Exception) {
        // In a production app, this could:
        // 1. Queue transaction for retry
        // 2. Send error report to analytics
        // 3. Show user notification if critical
        Log.w(TAG, "Transaction could not be saved - consider implementing retry mechanism")
    }
    
    private fun handleParserError(packageName: String, text: String, error: Exception) {
        // In a production app, this could:
        // 1. Send anonymized text to improve parser
        // 2. Track parser accuracy metrics
        // 3. Update parsing rules automatically
        Log.w(TAG, "Parser failed for $packageName - text might need new parsing rules")
    }
    
    private fun logErrorStats() {
        val processed = processedNotifications.get()
        val parseErr = parseErrors.get()
        val dbErr = dbErrors.get()
        
        if (processed > 0) {
            Log.i(TAG, "SMS Processing Stats - Total: $processed, Parse Errors: $parseErr, DB Errors: $dbErr")
            Log.i(TAG, "Success Rate: ${((processed - parseErr - dbErr).toFloat() / processed * 100).toInt()}%")
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