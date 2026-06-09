package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bank-related SMS/notification that looked like a transaction but the parser could not turn
 * into one. This is a product signal, not telemetry: the messages stay on-device so the user (and
 * future parser rules) can see exactly which formats are being missed. Nothing here ever touches a
 * balance — it is a passive diagnostic log surfaced in Settings → "Mensajes no reconocidos".
 *
 * [id] is a stable dedup hash (minute bucket + normalized text) so the same message replayed by the
 * notification listener on reconnect — or seen by both the SMS receiver and the listener — collapses
 * to a single row via INSERT IGNORE, mirroring the parser's own dedup strategy.
 */
@Entity(
    tableName = "unparsed_sms",
    indices = [Index(value = ["receivedAt"])]
)
data class UnparsedSms(
    @PrimaryKey
    val id: String,
    val text: String,
    val source: String,       // "sms" | "notif:<package>"
    val receivedAt: Long      // epoch millis
)
