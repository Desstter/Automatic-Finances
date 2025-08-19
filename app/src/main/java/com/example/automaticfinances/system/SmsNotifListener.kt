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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn?.notification ?: return
        val extras = n.extras
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "") +
                   " " +
                   (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "")

        // Escuchar TODO y parsear por contenido ("Bancolombia:")
        val tx = BancolombiaParser.tryParse(text) ?: return

        scope.launch { addTx(tx) }
    }
}