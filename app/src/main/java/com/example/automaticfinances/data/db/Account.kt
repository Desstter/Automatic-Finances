package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["type"]),
        Index(value = ["isActive"]),
        Index(value = ["name"], unique = true)
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // "Banco Bancolombia", "Efectivo"
    val type: AccountType,               // BANK, CASH
    val balanceCents: Long,              // Current balance in cents
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createBankAccount(name: String = "Banco", initialBalanceCents: Long = 0L): Account {
            return Account(
                name = name,
                type = AccountType.BANK,
                balanceCents = initialBalanceCents
            )
        }
        
        fun createCashAccount(name: String = "Efectivo", initialBalanceCents: Long = 0L): Account {
            return Account(
                name = name,
                type = AccountType.CASH,
                balanceCents = initialBalanceCents
            )
        }
    }
    
    val formattedBalance: String
        get() {
            val amount = balanceCents / 100.0
            return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(amount)
        }
    
    val isBank: Boolean get() = type == AccountType.BANK
    val isCash: Boolean get() = type == AccountType.CASH
}

enum class AccountType {
    BANK,    // Cuenta bancaria - transacciones SMS automáticas
    CASH     // Efectivo - transacciones manuales
}

data class AccountSummary(
    val totalBalanceCents: Long,
    val bankBalanceCents: Long,
    val cashBalanceCents: Long,
    val accountCount: Int,
    val activeAccountCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val bankPercentage: Float
        get() = if (totalBalanceCents == 0L) 0f 
                else (bankBalanceCents.toFloat() / totalBalanceCents.toFloat()) * 100f
    
    val cashPercentage: Float
        get() = if (totalBalanceCents == 0L) 0f 
                else (cashBalanceCents.toFloat() / totalBalanceCents.toFloat()) * 100f
}

data class BalanceHistory(
    val accountId: Long,
    val balanceCents: Long,
    val timestamp: Long,
    val transactionId: String?, // Transaction that caused this balance change
    val changeType: BalanceChangeType
)

enum class BalanceChangeType {
    INITIAL,      // Initial balance setup
    TRANSACTION,  // Transaction added/modified
    ADJUSTMENT,   // Manual balance adjustment
    CORRECTION    // System correction
}