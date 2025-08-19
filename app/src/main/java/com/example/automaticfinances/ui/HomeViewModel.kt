package com.example.automaticfinances.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeState(
    val items: List<Transaction> = emptyList(),
    val totalMonthCOP: Long = 0L
)

class HomeViewModel : ViewModel() {
    private val repo = TransactionRepository()
    private val now = System.currentTimeMillis()
    private val monthStart = LocalDate.now().withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val state = repo.all()
        .map { list ->
            val total = list.filter { it.ts in monthStart..now }.sumOf { it.amountCents }
            HomeState(items = list, totalMonthCOP = total)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}