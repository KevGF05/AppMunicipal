package com.example.appmunicipal.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class SeguimientoLocal(
    val codigo: String,
    val estado: String,
    val fecha: Long
)

class SeguimientoDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE seguimientos (
                codigo TEXT PRIMARY KEY,
                estado TEXT NOT NULL,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS seguimientos")
        onCreate(db)
    }

    fun guardar(codigo: String, estado: String) {
        val values = ContentValues().apply {
            put("codigo", codigo)
            put("estado", estado)
            put("fecha", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "seguimientos",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun ultimo(): SeguimientoLocal? {
        readableDatabase.query(
            "seguimientos",
            arrayOf("codigo", "estado", "fecha"),
            null,
            null,
            null,
            null,
            "fecha DESC",
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return SeguimientoLocal(
                codigo = cursor.getString(0),
                estado = cursor.getString(1),
                fecha = cursor.getLong(2)
            )
        }
    }

    companion object {
        private const val DATABASE_NAME = "app_municipal_local.db"
        private const val DATABASE_VERSION = 2
    }
}
