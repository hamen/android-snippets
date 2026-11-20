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

package com.example.snippets.storage.documents

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.BufferedReader
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

private const val TAG = "DocumentsFiles"

private fun persistPermissions(applicationContext: Context, uri: Uri) {
    // [START android_datastorage_documents_persist_permissions]
    val contentResolver = applicationContext.contentResolver

    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    // Check for the freshest data.
    contentResolver.takePersistableUriPermission(uri, takeFlags)
    // [END android_datastorage_documents_persist_permissions]
}

private fun examineDocumentMetadata(applicationContext: Context) {
    // [START android_datastorage_documents_examine_metadata]
    val contentResolver = applicationContext.contentResolver

    fun dumpImageMetaData(uri: Uri) {

        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        val cursor: Cursor? = contentResolver.query(
            uri, null, null, null, null, null
        )

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows. Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the filename.
                val displayName: String =
                    it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                Log.i(TAG, "Display Name: $displayName")

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                // If the size is unknown, the value stored is null. But because an
                // int can't be null, the behavior is implementation-specific,
                // and unpredictable. So as
                // a rule, check if it's null before assigning to an int. This will
                // happen often: The storage API allows for remote files, whose
                // size might not be locally known.
                val size: String = if (!it.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(TAG, "Size: $size")
            }
        }
    }
    // [END android_datastorage_documents_examine_metadata]
}

private class BitmapDocumentReader(applicationContext: Context) {
    // [START android_datastorage_documents_open_bitmap]
    val contentResolver = applicationContext.contentResolver

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException()
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }
    // [END android_datastorage_documents_open_bitmap]
}

private class TextDocumentReader(applicationContext: Context) {
    // [START android_datastorage_documents_open_input_stream]
    val contentResolver = applicationContext.contentResolver

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
    // [END android_datastorage_documents_open_input_stream]
}

private class DocumentEditor(applicationContext: Context) {
    // [START android_datastorage_documents_edit]
    val contentResolver = applicationContext.contentResolver

    private fun alterDocument(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    it.write(
                        ("Overwritten at ${System.currentTimeMillis()}\n")
                            .toByteArray()
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    // [END android_datastorage_documents_edit]
}

private fun deleteDocument(applicationContext: Context, uri: Uri) {
    // [START android_datastorage_documents_delete]
    DocumentsContract.deleteDocument(applicationContext.contentResolver, uri)
    // [END android_datastorage_documents_delete]
}

private class VirtualFileChecker(base: Context) : ContextWrapper(base) {
    // [START android_datastorage_documents_is_virtual_file]
    private fun isVirtualFile(uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(this, uri)) {
            return false
        }

        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null,
            null,
            null
        )

        val flags: Int = cursor?.use {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        } ?: 0

        return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }
    // [END android_datastorage_documents_is_virtual_file]

    // [START android_datastorage_documents_open_virtual_file]
    @Throws(IOException::class)
    private fun getInputStreamForVirtualFile(
        uri: Uri,
        mimeTypeFilter: String
    ): InputStream {

        val openableMimeTypes: Array<String>? =
            contentResolver.getStreamTypes(uri, mimeTypeFilter)

        return if (openableMimeTypes?.isNotEmpty() == true) {
            contentResolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                ?.createInputStream() ?: throw FileNotFoundException()
        } else {
            throw FileNotFoundException()
        }
    }
    // [END android_datastorage_documents_open_virtual_file]
}
