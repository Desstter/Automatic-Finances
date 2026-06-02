package com.example.automaticfinances.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.automaticfinances.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup / restore por copia directa del archivo SQLite de Room (`autobook.db`).
 *
 * Es la estrategia más completa y a prueba de bugs: respalda *todo* (transacciones,
 * categorías, saldos, presupuestos, metas, preferencias aprendidas, resoluciones de
 * comercio) junto con la versión de esquema, sin tener que serializar tabla por tabla.
 *
 * - Exportar: `wal_checkpoint(TRUNCATE)` pliega el WAL dentro del archivo principal y se
 *   copian los bytes al destino elegido por el usuario (SAF).
 * - Restaurar: se valida que el archivo entrante sea un SQLite legible y con las tablas
 *   del esquema, se cierra Room, se reemplaza el archivo, se borran `-wal`/`-shm` y se
 *   reinicia el proceso para que Room reabra limpio sobre los datos nuevos.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {

    fun suggestedFileName(): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        return "automaticfinances-$stamp.afbackup"
    }

    /** Copia la base de datos actual al [destination] elegido por el usuario. */
    @Throws(IOException::class)
    fun export(destination: Uri) {
        // Plegar el WAL dentro del archivo principal para que la copia quede completa.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use {
            it.moveToFirst()
        }
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val out = context.contentResolver.openOutputStream(destination)
            ?: throw IOException("No se pudo abrir el destino para escribir")
        out.use { output ->
            dbFile.inputStream().use { it.copyTo(output) }
        }
    }

    /**
     * Restaura desde [source]. Reemplaza la base de datos actual. Lanza si el archivo no es
     * una copia válida (no toca los datos existentes hasta haber validado el entrante).
     */
    @Throws(IOException::class)
    fun import(source: Uri) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val temp = File(context.cacheDir, "restore_${System.currentTimeMillis()}.db")

        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
            } ?: throw IOException("No se pudo leer el archivo seleccionado")

            validate(temp)

            // Solo después de validar tocamos la base real.
            database.close()
            temp.copyTo(dbFile, overwrite = true)
            // El WAL/SHM antiguos pertenecen a la base anterior: hay que descartarlos.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
        } finally {
            temp.delete()
        }
    }

    /** Verifica que [file] sea un SQLite íntegro con las tablas del esquema de la app. */
    @Throws(IOException::class)
    private fun validate(file: File) {
        if (file.length() < 16) throw IOException("El archivo está vacío o dañado")
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                val ok = cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
                if (!ok) throw IOException("La copia de seguridad está dañada")
            }
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'",
                null,
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IOException("El archivo no es una copia de AutomaticFinances")
                }
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("No se pudo leer la copia de seguridad", e)
        } finally {
            db?.close()
        }
    }

    /** Reinicia el proceso para que Room reabra sobre los datos restaurados. */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
