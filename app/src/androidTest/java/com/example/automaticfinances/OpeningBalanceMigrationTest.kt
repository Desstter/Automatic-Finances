package com.example.automaticfinances

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.db.MIGRATION_13_14
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * FIN-4 — the one instrumentation test that earns its keep: proving the 13->14 migration corrects
 * the retroactive opening-balance bias WITHOUT corrupting data it shouldn't touch.
 *
 * Setup mirrors a real biased install: an auto-seeded opening balance whose stored value is the
 * END-of-effectiveDate balance (today's movements already baked in). After the migration the seed
 * must be the START-of-effectiveDate balance, so calculateCurrentBalanceWithOpening() — which adds
 * movements inclusive of effectiveDate — lands on the right number instead of double-counting.
 */
@RunWith(AndroidJUnit4::class)
class OpeningBalanceMigrationTest {

    private val testDb = "fin4-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate13To14_reseatsAutoSeededOpeningBalance_toStartOfDay() {
        val effectiveDate = "2026-05-30"

        helper.createDatabase(testDb, 13).apply {
            // Two accounts: 1 = Banco (biased auto-seed), 2 = Efectivo (user-configured, untouched).
            insertAccount(this, id = 1, name = "Banco", type = "BANK", balanceCents = 1_000_000)
            insertAccount(this, id = 2, name = "Efectivo", type = "CASH", balanceCents = 50_000)

            // Account 1 movements ON effectiveDate: +100_000 income, -30_000 expense => net +70_000.
            // These are the ones double-counted by the bias and must be subtracted from the seed.
            insertTx(this, id = "a1-inc", date = effectiveDate, accountId = 1, amountCents = 100_000, isIncome = 1)
            insertTx(this, id = "a1-exp", date = effectiveDate, accountId = 1, amountCents = 30_000, isIncome = 0)

            // Account 1 movement on a DIFFERENT day: must NOT be subtracted from the seed.
            insertTx(this, id = "a1-prev", date = "2026-05-29", accountId = 1, amountCents = 999_999, isIncome = 0)

            // Account 2 movement on effectiveDate: proves cross-account isolation (must not leak
            // into account 1's correction, and account 2's seed is user-configured so untouched).
            insertTx(this, id = "a2-inc", date = effectiveDate, accountId = 2, amountCents = 12_345, isIncome = 1)

            // Biased auto-seed for account 1: stored as the end-of-day balance (1_000_000).
            insertOpeningBalance(
                this, accountId = 1, effectiveDate = effectiveDate, balanceCents = 1_000_000,
                note = "Balance inicial automático"
            )
            // User-configured seed for account 2: must survive the migration unchanged.
            insertOpeningBalance(
                this, accountId = 2, effectiveDate = effectiveDate, balanceCents = 50_000,
                note = "Balance inicial configurado por usuario"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 14, true, MIGRATION_13_14)

        // Account 1 (auto-seeded): 1_000_000 - net(+70_000 on effectiveDate) = 930_000.
        assertEquals(930_000L, openingBalanceOf(db, accountId = 1))
        // Account 2 (user-configured): untouched despite a same-day movement.
        assertEquals(50_000L, openingBalanceOf(db, accountId = 2))
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate13To14_isNoOp_whenNoMovementsOnEffectiveDate() {
        helper.createDatabase(testDb, 13).apply {
            insertAccount(this, id = 1, name = "Banco", type = "BANK", balanceCents = 250_000)
            // Only a movement on a different day; nothing on the seed's effectiveDate.
            insertTx(this, id = "t-prev", date = "2026-05-29", accountId = 1, amountCents = 80_000, isIncome = 0)
            insertOpeningBalance(
                this, accountId = 1, effectiveDate = "2026-05-30", balanceCents = 250_000,
                note = "Balance inicial automático"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 14, true, MIGRATION_13_14)
        // No same-day net => seed is unchanged (already correct).
        assertEquals(250_000L, openingBalanceOf(db, accountId = 1))
        db.close()
    }

    // ---- helpers ----

    private fun insertAccount(db: SupportSQLiteDatabase, id: Long, name: String, type: String, balanceCents: Long) {
        db.execSQL(
            "INSERT INTO accounts (id, name, type, balanceCents, isActive, createdAt) VALUES (?, ?, ?, ?, 1, 0)",
            arrayOf<Any>(id, name, type, balanceCents)
        )
    }

    private fun insertTx(
        db: SupportSQLiteDatabase,
        id: String,
        date: String,
        accountId: Long,
        amountCents: Long,
        isIncome: Int
    ) {
        db.execSQL(
            """
            INSERT INTO transactions
                (id, ts, date, time, type, description, amountCents, currency, srcLast4, dstLast4,
                 source, categoryId, accountId, notes, isIncome, rawPreview)
            VALUES (?, 0, ?, '00:00', 'COMPRA', 'test', ?, 'COP', NULL, NULL, 'test', NULL, ?, '', ?, '')
            """,
            arrayOf<Any>(id, date, amountCents, accountId, isIncome)
        )
    }

    private fun insertOpeningBalance(
        db: SupportSQLiteDatabase,
        accountId: Long,
        effectiveDate: String,
        balanceCents: Long,
        note: String
    ) {
        db.execSQL(
            "INSERT INTO opening_balances (accountId, effectiveDate, balanceCents, note, isActive, createdAt) " +
                "VALUES (?, ?, ?, ?, 1, 0)",
            arrayOf<Any>(accountId, effectiveDate, balanceCents, note)
        )
    }

    private fun openingBalanceOf(db: SupportSQLiteDatabase, accountId: Long): Long {
        db.query(
            "SELECT balanceCents FROM opening_balances WHERE accountId = ? AND isActive = 1",
            arrayOf<Any>(accountId)
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }
}
