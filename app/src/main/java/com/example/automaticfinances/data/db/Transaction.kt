package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,          // hash dedupe
    val ts: Long,                        // epoch millis
    val type: String,                    // "COMPRA" | "TRANSFERENCIA"
    val amountCents: Long,               // COP *100
    val currency: String,                // "COP"
    val merchant: String?,               // en compras
    val srcLast4: String?,               // *1233
    val dstLast4: String?,               // para transferencia
    val source: String,                  // "notif:sms"
    val rawPreview: String               // primeros 140 chars
)