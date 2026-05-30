package com.example.automaticfinances.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.example.automaticfinances.data.parse.BancolombiaParser
import com.example.automaticfinances.domain.AddTransactionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class SmsNotifListener : NotificationListenerService() {

    // Recreated on (re)connect: a NotificationListenerService can be unbound and rebound on the
    // same instance, so a permanently-cancelled scope would silently stop processing after the
    // first disconnect. See onListenerConnected / onListenerDisconnected.
    private var scope = newScope()
    @Inject lateinit var addTx: AddTransactionUseCase

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
        // Revive the scope if a previous disconnect cancelled it (same instance can be rebound).
        if (!scope.isActive) {
            scope = newScope()
        }
        // Ensure foreground service is running when listener connects
        ForegroundSmsService.startService(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Try to restart the service if disconnected
        scope.cancel()
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

        // postTime identifies the notification deterministically: the same SMS/notification
        // re-delivered (on reconnect, update, etc.) keeps the same postTime, which the parser
        // folds into the transaction id so we don't create duplicates.
        val postTime = sbn.postTime

        // Filtrar por apps bancarias conocidas o contenido relevante
        if (shouldProcessNotification(packageName, text)) {
            processedNotifications.incrementAndGet()

            scope.launch {
                try {
                    processNotificationSafely(packageName, text, postTime)
                } catch (e: Exception) {
                    // Log critical failures but don't crash the service
                    Log.e(TAG, "Critical failure processing notification from $packageName", e)
                    logErrorStats()
                }
            }
        }
    }
    
    private suspend fun processNotificationSafely(packageName: String, text: String, postTime: Long) {
        try {
            // Attempt to parse the SMS text
            val transaction = BancolombiaParser.tryParse(text, postTime)
            
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
        // Bank/fintech apps that push transaction notifications directly.
        // Matched by EXACT package name — never by substring, otherwise "android"
        // would match almost every Google/system package (Gmail, Play, GMS, …)
        // and flood the parser with unrelated notifications.
        if (packageName in BANK_APPS) {
            return true
        }

        // SMS shows up as a notification from the device messaging app, whose package
        // varies by OEM. For those we additionally require bank-related content so we
        // don't treat arbitrary chat messages as transactions.
        if (packageName in MESSAGING_APPS) {
            return BANK_KEYWORDS.any { text.contains(it, ignoreCase = true) }
        }

        return false
    }

    private val BANK_APPS = setOf(
        "com.bancolombia.androidapp",      // Bancolombia oficial
        "com.mobile.bancolombia",          // Bancolombia alternativo
        "com.todo1.mobile",                // Bancolombia (app Personas)
        "com.nequi.MobileApp",             // Nequi
        "com.davivienda.daviplata",        // DaviPlata
        "co.com.ach.pse.app.avianca",      // Avianca LifeMiles
        "com.bbva.netcash",                // BBVA Colombia
        "com.bancodebogota.digital"        // Banco de Bogotá
    )

    // Common SMS/messaging app packages across OEMs.
    private val MESSAGING_APPS = setOf(
        "com.google.android.apps.messaging", // Google Messages (AOSP/Pixel default)
        "com.android.messaging",             // AOSP Messaging
        "com.android.mms",                   // Legacy MMS/SMS
        "com.samsung.android.messaging",     // Samsung Messages
        "com.xiaomi.mms",                    // Xiaomi
        "com.miui.mms",                      // MIUI
        "com.motorola.messaging",            // Motorola
        "com.oneplus.mms"                    // OnePlus
    )

    private val BANK_KEYWORDS = listOf(
        "bancolombia", "compraste", "transferiste", "transferencia",
        "nequi", "pagaste", "daviplata", "transacción", "débito",
        "compra", "retiro", "consignación", "pago", "recibiste", "retiraste"
    )
}