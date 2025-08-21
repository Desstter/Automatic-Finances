package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(
    tableName = "opening_balances",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["effectiveDate"]),
        Index(value = ["isActive"]),
        Index(value = ["accountId", "effectiveDate"]),
        Index(value = ["accountId", "isActive"])
    ]
)
data class OpeningBalance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,                    // FK to accounts (bank/cash)
    val effectiveDate: String,              // "2024-08-19" format (YYYY-MM-DD)
    val balanceCents: Long,                 // Opening balance in cents
    val note: String = "",                  // Optional description/reason
    val isActive: Boolean = true,           // For history tracking - only one active per account
    val createdAt: Long = System.currentTimeMillis()
) {
    
    companion object {
        fun create(
            accountId: Long,
            effectiveDate: LocalDate,
            balanceCents: Long,
            note: String = ""
        ): OpeningBalance {
            return OpeningBalance(
                accountId = accountId,
                effectiveDate = effectiveDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                balanceCents = balanceCents,
                note = note
            )
        }
        
        fun createForAccount(
            account: Account,
            effectiveDate: LocalDate,
            balanceCents: Long,
            note: String = "Balance inicial"
        ): OpeningBalance {
            return create(
                accountId = account.id,
                effectiveDate = effectiveDate,
                balanceCents = balanceCents,
                note = note
            )
        }
    }
    
    // Convenience properties
    val formattedBalance: String
        get() {
            val amount = balanceCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val effectiveDateAsLocalDate: LocalDate
        get() = LocalDate.parse(effectiveDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    
    val balanceInPesos: Double
        get() = balanceCents / 100.0
    
    val isPositiveBalance: Boolean
        get() = balanceCents >= 0
    
    val formattedEffectiveDate: String
        get() {
            val date = effectiveDateAsLocalDate
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
}

// Data class for calculations combining opening balance with current state
data class AccountWithOpeningBalance(
    val account: Account,
    val openingBalance: OpeningBalance?,
    val currentBalanceCents: Long,
    val transactionsSinceOpening: Int = 0
) {
    val hasOpeningBalance: Boolean
        get() = openingBalance != null
    
    val openingBalanceCents: Long
        get() = openingBalance?.balanceCents ?: 0L
    
    val netChangeCents: Long
        get() = currentBalanceCents - openingBalanceCents
    
    val formattedCurrentBalance: String
        get() {
            val amount = currentBalanceCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val formattedOpeningBalance: String
        get() = openingBalance?.formattedBalance ?: "$0"
    
    val formattedNetChange: String
        get() {
            val amount = netChangeCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val hasGrowth: Boolean
        get() = netChangeCents > 0
    
    val hasDecline: Boolean
        get() = netChangeCents < 0
    
    val isStable: Boolean
        get() = netChangeCents == 0L
}

// Summary for all accounts combined
data class OpeningBalanceSummary(
    val totalOpeningBalanceCents: Long,
    val totalCurrentBalanceCents: Long,
    val totalNetChangeCents: Long,
    val bankOpeningBalanceCents: Long,
    val cashOpeningBalanceCents: Long,
    val bankCurrentBalanceCents: Long,
    val cashCurrentBalanceCents: Long,
    val accountsWithOpeningBalance: Int,
    val effectiveDate: String? = null, // Most recent effective date
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val hasPositiveGrowth: Boolean
        get() = totalNetChangeCents > 0
    
    val formattedTotalOpening: String
        get() {
            val amount = totalOpeningBalanceCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val formattedTotalCurrent: String
        get() {
            val amount = totalCurrentBalanceCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val formattedTotalChange: String
        get() {
            val amount = totalNetChangeCents / 100.0
            return NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(amount)
        }
    
    val growthPercentage: Float
        get() = if (totalOpeningBalanceCents == 0L) 0f
                else (totalNetChangeCents.toFloat() / totalOpeningBalanceCents.toFloat()) * 100f
}