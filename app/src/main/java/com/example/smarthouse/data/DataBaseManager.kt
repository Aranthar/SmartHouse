package com.example.smarthouse.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class DataBaseManager(context: Context) {
    private val dataBaseHelper = DataBaseHelper(context)
    private var db: SQLiteDatabase? = null

    fun openDataBase() {
        db = dataBaseHelper.writableDatabase
    }

    fun insertToDataBase(type: String, content: String) {
        val values = ContentValues().apply {
            put(DataBaseSQLite.COLUMN_NAME_TITLE, type)
            put(DataBaseSQLite.COLUMN_NAME_CONTENT, content)
        }

        db?.insert(DataBaseSQLite.TABLE_NAME, null, values)
    }

    fun readDataBaseData(): ArrayList<List<String?>> {
        val dataList = ArrayList<List<String?>>()
        val cursor = db?.query(
            DataBaseSQLite.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null,
        )

        with (cursor) {
            while (this?.moveToNext()!!) {
                val dataTitle = cursor?.getString(cursor.getColumnIndexOrThrow(DataBaseSQLite.COLUMN_NAME_TITLE))
                val dataContent = cursor?.getString(cursor.getColumnIndexOrThrow(DataBaseSQLite.COLUMN_NAME_CONTENT))
                dataList.add(listOf(dataTitle, dataContent))
            }
        }
        cursor?.close()

        return dataList
    }

    fun updateContent(type: String, content: String) {
        db = dataBaseHelper.writableDatabase

        val values = ContentValues().apply {
            put(DataBaseSQLite.COLUMN_NAME_TITLE, type)
            put(DataBaseSQLite.COLUMN_NAME_CONTENT, content)
        }

        db?.update(DataBaseSQLite.TABLE_NAME, values, "type = ?", arrayOf(type))
    }

    fun deleteContent(type: String) {
        db = dataBaseHelper.writableDatabase
        db?.delete(DataBaseSQLite.TABLE_NAME, "type = ?", arrayOf(type))
    }
}