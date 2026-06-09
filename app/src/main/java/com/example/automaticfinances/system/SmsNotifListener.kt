package com.example.automaticfinances.system

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.automaticfinances.data.parse.BancolombiaParser
import com.example.automaticfinances.data.repo.UnparsedSmsRepository
import com.example.automaticfinances.domain.CaptureTransactionUseCase
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
    @Inject lateinit var captureTx: CaptureTransactionUseCase
    @Inject lateinit var unparsedRepo: UnparsedSmsRepository

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val parseErrors = AtomicInteger(0)
    private val dbErrors = AtomicInteger(0)
    private val processedNotifications = AtomicInteger(0)

    companion object {
        private const val TAG = "SmsNotifListener"
        private const val MAX_TEXT_LENGTH = 1000
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!scope.isActive) {
            scope = newScope()
        }
        // Keep the process alive via the dedicated anchor service, NOT by promoting this listener to
        // foreground: a listener-hosted FGS dies the moment HyperOS unbinds the listener, which is
        // exactly when we most need the process to survive. CaptureKeepAliveService is independent of
        // the binding and outlives it. Starting it here (belt-and-suspenders) guarantees the anchor is
        // running whenever the listener is live, even if App.onCreate's start was throttled.
        CaptureKeepAliveService.start(this)

        runCatching { VoiceQuickActionNotifier.show(this) }
        reprocessActiveNotifications()
    }

    private fun reprocessActiveNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        for (sbn in active) {
            runCatching { handleNotification(sbn) }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        scope.cancel()
        // Do NOT stop the foreground anchor here: CaptureKeepAliveService is independent of this
        // binding and must keep the process alive precisely while the listener is down, so the OS can
        // rebind us (and so the SMS broadcast path stays in a warm process). Just ask to rebind.
        requestRebind(ComponentName(this, SmsNotifListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        handleNotification(sbn)
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val packageName = sbn.packageName
        val extras = n.extras

        // Standard text fields
        val mainText = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "") +
                       " " +
                       (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "")

        // MessagingStyle (e.g. Nu, WhatsApp) stores content in EXTRA_MESSAGES as Bundle[],
        // not in EXTRA_TEXT/EXTRA_BIG_TEXT. Without this, the assembled text would be empty
        // and the notification would be silently dropped.
        val messagesText = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.mapNotNull { (it as? android.os.Bundle)?.getCharSequence("text")?.toString() }
                ?.joinToString(" ")
                ?.trim()
                .orEmpty()
        }.getOrElse { "" }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = "$mainText $messagesText $title".trim()

        val postTime = sbn.postTime

        when (classify(packageName, text)) {
            Action.PROCESS -> {
                processedNotifications.incrementAndGet()
                scope.launch {
                    try {
                        processNotificationSafely(packageName, text, postTime)
                    } catch (e: Exception) {
                        Log.e(TAG, "Critical failure processing notification from $packageName", e)
                        logErrorStats()
                    }
                }
            }
            Action.LOG_ONLY -> {
                scope.launch {
                    runCatching { unparsedRepo.record(text, source = "notif:$packageName", receivedAt = postTime) }
                }
            }
            Action.IGNORE -> Unit
        }
    }

    private suspend fun processNotificationSafely(packageName: String, text: String, postTime: Long) {
        try {
            val transaction = BancolombiaParser.tryParse(text, postTime)

            if (transaction == null) {
                Log.d(TAG, "No transaction parsed from $packageName")
                // Known bank apps: always log failures — any notification from them is financially
                // relevant regardless of content. Unknown sources: gate on looksLikeTransaction so
                // we don't flood the log with OTPs or promos.
                val shouldLog = packageName in BANK_APPS ||
                    UnparsedSmsRepository.looksLikeTransaction(text)
                if (shouldLog && text.isNotBlank()) {
                    unparsedRepo.record(text, source = "notif:$packageName", receivedAt = postTime)
                }
                return
            }

            try {
                captureTx(transaction)
                Log.d(TAG, "Successfully processed ${transaction.type} transaction from $packageName")
            } catch (e: Exception) {
                dbErrors.incrementAndGet()
                Log.e(TAG, "Database error saving ${transaction.type} transaction from $packageName", e)
                handleDatabaseError(transaction, e)
            }

        } catch (e: Exception) {
            parseErrors.incrementAndGet()
            Log.e(TAG, "Parser error for notification from $packageName", e)
            handleParserError(packageName, text.take(MAX_TEXT_LENGTH), e)
        }
    }

    private fun handleDatabaseError(transaction: com.example.automaticfinances.data.db.Transaction, error: Exception) {
        Log.w(TAG, "Transaction could not be saved - consider implementing retry mechanism")
    }

    private fun handleParserError(packageName: String, text: String, error: Exception) {
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

    private enum class Action { PROCESS, LOG_ONLY, IGNORE }

    private fun classify(packageName: String, text: String): Action {
        if (packageName == this.packageName) return Action.IGNORE
        if (packageName in BANK_APPS) return Action.PROCESS
        if (packageName in MESSAGING_APPS) {
            return if (UnparsedSmsRepository.looksLikeTransaction(text)) Action.PROCESS else Action.IGNORE
        }
        return if (UnparsedSmsRepository.looksLikeTransaction(text)) Action.LOG_ONLY else Action.IGNORE
    }

    private val BANK_APPS = setOf(
        "com.bancolombia.androidapp",
        "com.mobile.bancolombia",
        "com.todo1.mobile",
        "com.nequi.MobileApp",
        "com.davivienda.daviplata",
        "com.nu.production",
        "co.com.ach.pse.app.avianca",
        "com.bbva.netcash",
        "com.bancodebogota.digital"
    )

    private val MESSAGING_APPS = setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.xiaomi.mms",
        "com.miui.mms",
        "com.motorola.messaging",
        "com.oneplus.mms"
    )
}
