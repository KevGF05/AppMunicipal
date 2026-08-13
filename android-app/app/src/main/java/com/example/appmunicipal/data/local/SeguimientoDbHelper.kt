package com.example.appmunicipal.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class SeguimientoLocal(
    val codigo: String,
    val estado: String,
    val fecha: Long,
    val tipo: String?
)

/**
 * Guarda en el telefono los codigos de seguimiento enviados desde este dispositivo.
 *
 * Dos correcciones importantes respecto a la version anterior:
 *
 *  1. onUpgrade ya no hace DROP TABLE. El codigo de seguimiento es el unico vinculo
 *     del ciudadano con su reporte: borrarlo al subir la version de la base era
 *     perder la unica referencia que tenia.
 *  2. Se agrego todos(), porque la tabla siempre guardo varios codigos pero la app
 *     solo leia el ultimo.
 */
class SeguimientoDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS seguimientos (
                codigo TEXT PRIMARY KEY,
                estado TEXT NOT NULL,
                fecha INTEGER NOT NULL,
                tipo TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migracion incremental: se conservan los codigos ya guardados.
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS seguimientos (
                    codigo TEXT PRIMARY KEY,
                    estado TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    tipo TEXT
                )
                """.trimIndent()
            )
            // La columna tipo no existia en la version 2. Si ya existe, se ignora el error.
            runCatching { db.execSQL("ALTER TABLE seguimientos ADD COLUMN tipo TEXT") }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Instalar una version anterior no debe borrar los datos ni hacer crashear la app.
    }

    /**
     * Inserta o actualiza un codigo. Si no se envia [tipo] se conserva el que ya
     * estuviera guardado, para que refrescar el estado no borre el tipo de reporte.
     */
    fun guardar(codigo: String, estado: String, tipo: String? = null) {
        val tipoFinal = tipo ?: obtener(codigo)?.tipo
        val values = ContentValues().apply {
            put("codigo", codigo)
            put("estado", estado)
            put("fecha", System.currentTimeMillis())
            put("tipo", tipoFinal)
        }
        writableDatabase.insertWithOnConflict(
            "seguimientos",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun obtener(codigo: String): SeguimientoLocal? {
        readableDatabase.query(
            "seguimientos",
            COLUMNAS,
            "codigo = ?",
            arrayOf(codigo),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.leer()
        }
    }

    fun ultimo(): SeguimientoLocal? = todos().firstOrNull()

    /** Todos los codigos guardados, del mas reciente al mas antiguo. */
    fun todos(): List<SeguimientoLocal> {
        val resultado = mutableListOf<SeguimientoLocal>()
        readableDatabase.query(
            "seguimientos",
            COLUMNAS,
            null,
            null,
            null,
            null,
            "fecha DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(cursor.leer())
            }
        }
        return resultado
    }

    fun eliminar(codigo: String) {
        writableDatabase.delete("seguimientos", "codigo = ?", arrayOf(codigo))
    }

    private fun android.database.Cursor.leer() = SeguimientoLocal(
        codigo = getString(0),
        estado = getString(1),
        fecha = getLong(2),
        tipo = if (isNull(3)) null else getString(3)
    )

    companion object {
        private const val DATABASE_NAME = "app_municipal_local.db"

        // 2 -> 3: se agrega la columna tipo y se deja de borrar la tabla al migrar.
        private const val DATABASE_VERSION = 3

        private val COLUMNAS = arrayOf("codigo", "estado", "fecha", "tipo")
    }
}
