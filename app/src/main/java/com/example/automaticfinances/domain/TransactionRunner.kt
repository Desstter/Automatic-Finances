package com.example.automaticfinances.domain

/**
 * Runs a block of work inside a single database transaction so that multi-step money
 * operations are atomic: either every write (row inserts + balance adjustments) commits, or
 * none does. This protects the financial invariant when an operation touches more than one row
 * (e.g. the ATM withdrawal dual-entry) or performs an insert followed by a balance adjustment.
 *
 * Abstracted behind an interface so use cases stay unit-testable without a real Room database.
 */
interface TransactionRunner {
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}
