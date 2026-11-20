/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.snippets.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.snippets.sqlite.FeedReaderContract.FeedEntry

// [START android_datastorage_sqlite_contract]
object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object FeedEntry : BaseColumns {
        const val TABLE_NAME = "entry"
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_SUBTITLE = "subtitle"
    }
}
// [END android_datastorage_sqlite_contract]

// [START android_datastorage_sqlite_sql_statements]
private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE ${FeedEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedEntry.COLUMN_NAME_TITLE} TEXT," +
        "${FeedEntry.COLUMN_NAME_SUBTITLE} TEXT)"

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedEntry.TABLE_NAME}"
// [END android_datastorage_sqlite_sql_statements]

// [START android_datastorage_sqlite_db_helper]
class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over.
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FeedReader.db"
    }
}
// [END android_datastorage_sqlite_db_helper]

private fun instantiateHelper(context: Context) {
    // [START android_datastorage_sqlite_instantiate_helper]
    val dbHelper = FeedReaderDbHelper(context)
    // [END android_datastorage_sqlite_instantiate_helper]
}

private fun insertRow(dbHelper: FeedReaderDbHelper, title: String, subtitle: String) {
    // [START android_datastorage_sqlite_insert]
    // Gets the data repository in write mode.
    val db = dbHelper.writableDatabase

    // Create a new map of values, where column names are the keys.
    val values = ContentValues().apply {
        put(FeedEntry.COLUMN_NAME_TITLE, title)
        put(FeedEntry.COLUMN_NAME_SUBTITLE, subtitle)
    }

    // Insert the new row, returning the primary key value of the new row.
    val newRowId = db?.insert(FeedEntry.TABLE_NAME, null, values)
    // [END android_datastorage_sqlite_insert]
}

private fun readFromDatabase(dbHelper: FeedReaderDbHelper) {
    // [START android_datastorage_sqlite_query]
    val db = dbHelper.readableDatabase

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    val projection = arrayOf(BaseColumns._ID, FeedEntry.COLUMN_NAME_TITLE, FeedEntry.COLUMN_NAME_SUBTITLE)

    // Filter results WHERE "title" = 'My Title'.
    val selection = "${FeedEntry.COLUMN_NAME_TITLE} = ?"
    val selectionArgs = arrayOf("My Title")

    // How you want the results sorted in the resulting Cursor.
    val sortOrder = "${FeedEntry.COLUMN_NAME_SUBTITLE} DESC"

    val cursor = db.query(
        FeedEntry.TABLE_NAME, // The table to query.
        projection, // The array of columns to return (pass null to get all).
        selection, // The columns for the WHERE clause.
        selectionArgs, // The values for the WHERE clause.
        null, // don't group the rows.
        null, // don't filter by row groups.
        sortOrder // The sort order.
    )
    // [END android_datastorage_sqlite_query]

    // [START android_datastorage_sqlite_read_cursor]
    val itemIds = mutableListOf<Long>()
    with(cursor) {
        while (moveToNext()) {
            val itemId = getLong(getColumnIndexOrThrow(BaseColumns._ID))
            itemIds.add(itemId)
        }
    }
    cursor.close()
    // [END android_datastorage_sqlite_read_cursor]
}

private fun deleteRows(db: SQLiteDatabase) {
    // [START android_datastorage_sqlite_delete]
    // Define 'where' part of query.
    val selection = "${FeedEntry.COLUMN_NAME_TITLE} LIKE ?"
    // Specify arguments in placeholder order.
    val selectionArgs = arrayOf("MyTitle")
    // Issue SQL statement.
    val deletedRows = db.delete(FeedEntry.TABLE_NAME, selection, selectionArgs)
    // [END android_datastorage_sqlite_delete]
}

private fun updateDatabase(dbHelper: FeedReaderDbHelper) {
    // [START android_datastorage_sqlite_update]
    val db = dbHelper.writableDatabase

    // New value for one column.
    val title = "MyNewTitle"
    val values = ContentValues().apply {
        put(FeedEntry.COLUMN_NAME_TITLE, title)
    }

    // Which row to update, based on the title.
    val selection = "${FeedEntry.COLUMN_NAME_TITLE} LIKE ?"
    val selectionArgs = arrayOf("MyOldTitle")
    val count = db.update(
        FeedEntry.TABLE_NAME,
        values,
        selection,
        selectionArgs
    )
    // [END android_datastorage_sqlite_update]
}
