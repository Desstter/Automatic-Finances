package com.example.automaticfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla categories
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                icon TEXT NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1
            )
        """)
        
        // 2. Poblar categorías por defecto
        val defaultCategories = listOf(
            "('Comida obligatoria', '#4CAF50', '🍽️', 1, 1)",
            "('Arriendo', '#2196F3', '🏠', 1, 1)",
            "('Salud', '#F44336', '🏥', 1, 1)",
            "('Comida por fuera', '#FF9800', '🍔', 1, 1)",
            "('Gasolina', '#795548', '⛽', 1, 1)",
            "('Transporte', '#607D8B', '🚗', 1, 1)",
            "('Entretenimiento', '#9C27B0', '🎬', 1, 1)",
            "('Ropa', '#E91E63', '👕', 1, 1)",
            "('Servicios', '#3F51B5', '🔧', 1, 1)",
            "('Otros', '#9E9E9E', '📦', 1, 1)"
        )
        
        for (category in defaultCategories) {
            database.execSQL("INSERT INTO categories (name, color, icon, isDefault, isActive) VALUES $category")
        }
        
        // 3. Crear nueva tabla transactions temporal
        database.execSQL("""
            CREATE TABLE transactions_new (
                id TEXT PRIMARY KEY NOT NULL,
                ts INTEGER NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                srcLast4 TEXT,
                dstLast4 TEXT,
                source TEXT NOT NULL,
                categoryId INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                rawPreview TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)
        
        // 4. Migrar datos existentes
        // Primero obtenemos todos los datos de la tabla vieja
        database.execSQL("""
            INSERT INTO transactions_new (
                id, ts, date, time, type, description, amountCents, 
                currency, srcLast4, dstLast4, source, categoryId, notes, rawPreview
            )
            SELECT 
                id, 
                ts,
                CASE 
                    WHEN ts > 0 THEN date(ts/1000, 'unixepoch', 'localtime')
                    ELSE '2024-01-01'
                END as date,
                CASE 
                    WHEN ts > 0 THEN time(ts/1000, 'unixepoch', 'localtime')
                    ELSE '00:00'
                END as time,
                type,
                COALESCE(merchant, 'Transacción') as description,
                amountCents,
                currency,
                srcLast4,
                dstLast4,
                source,
                NULL as categoryId,
                '' as notes,
                rawPreview
            FROM transactions
        """)
        
        // 5. Eliminar tabla vieja y renombrar
        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        
        // 6. Crear índices para mejor performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
    }
}