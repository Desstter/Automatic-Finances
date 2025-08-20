package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["isIncome"]),
        Index(value = ["date", "isIncome"])
    ]
)
data class Transaction(
    @PrimaryKey val id: String,          // hash dedupe (mantenemos compatibilidad)
    val ts: Long,                        // epoch millis (mantenemos para compatibilidad)
    val date: String,                    // "2024-08-19" (nuevo campo legible)
    val time: String,                    // "14:35" (nuevo campo legible)
    val type: String,                    // "COMPRA" | "TRANSFERENCIA"
    val description: String,             // Nombre del merchant o descripción (antes "merchant")
    val amountCents: Long,               // COP *100
    val currency: String,                // "COP"
    val srcLast4: String?,               // *1233
    val dstLast4: String?,               // para transferencia
    val source: String,                  // "notif:sms"
    val categoryId: Long? = null,        // FK a categories (nuevo)
    val notes: String = "",              // Notas adicionales del usuario (nuevo)
    val isIncome: Boolean = false,       // true para ingresos, false para gastos
    val rawPreview: String               // primeros 140 chars del SMS original
) {
    companion object {
        // Helper para crear desde timestamp
        fun fromTimestamp(
            id: String,
            ts: Long,
            type: String,
            description: String,
            amountCents: Long,
            currency: String,
            srcLast4: String?,
            dstLast4: String?,
            source: String,
            rawPreview: String,
            categoryId: Long? = null,
            isIncome: Boolean = false
        ): Transaction {
            val instant = Instant.ofEpochMilli(ts)
            val zonedDateTime = instant.atZone(ZoneId.of("America/Bogota"))
            val date = zonedDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = zonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            
            return Transaction(
                id = id,
                ts = ts,
                date = date,
                time = time,
                type = type,
                description = description,
                amountCents = amountCents,
                currency = currency,
                srcLast4 = srcLast4,
                dstLast4 = dstLast4,
                source = source,
                categoryId = categoryId,
                isIncome = isIncome,
                rawPreview = rawPreview
            )
        }
    }
}