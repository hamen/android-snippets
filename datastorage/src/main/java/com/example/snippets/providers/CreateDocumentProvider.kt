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

package com.example.snippets.providers

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.snippets.datastorage.R
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.PriorityQueue

// [START android_datastorage_document_provider_projections]
private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
    DocumentsContract.Root.COLUMN_ROOT_ID,
    DocumentsContract.Root.COLUMN_MIME_TYPES,
    DocumentsContract.Root.COLUMN_FLAGS,
    DocumentsContract.Root.COLUMN_ICON,
    DocumentsContract.Root.COLUMN_TITLE,
    DocumentsContract.Root.COLUMN_SUMMARY,
    DocumentsContract.Root.COLUMN_DOCUMENT_ID,
    DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
)
private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    DocumentsContract.Document.COLUMN_FLAGS,
    DocumentsContract.Document.COLUMN_SIZE
)
// [END android_datastorage_document_provider_projections]

private fun notifyRootsChanged(context: Context) {
    // [START android_datastorage_document_provider_notify_roots]
    val rootsUri: Uri = DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY)
    context.contentResolver.notifyChange(rootsUri, null)
    // [END android_datastorage_document_provider_notify_roots]
}

private class MyCloudProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    // [START android_datastorage_document_provider_query_roots]
    override fun queryRoots(projection: Array<out String>?): Cursor {
        // Use a MatrixCursor to build a cursor
        // with either the requested fields, or the default
        // projection if "projection" is null.
        val result = MatrixCursor(resolveRootProjection(projection))

        // If user is not logged in, return an empty root cursor.  This removes our
        // provider from the list entirely.
        if (!isUserLoggedIn()) {
            return result
        }

        // It's possible to have multiple roots (e.g. for multiple accounts in the
        // same app) -- just add multiple cursor rows.
        result.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT)

            // You can provide an optional summary, which helps distinguish roots
            // with the same title. You can also use this field for displaying an
            // user account name.
            add(DocumentsContract.Root.COLUMN_SUMMARY, requireContext().getString(R.string.root_summary))

            // FLAG_SUPPORTS_CREATE means at least one directory under the root supports
            // creating documents. FLAG_SUPPORTS_RECENTS means your application's most
            // recently used documents will show up in the "Recents" category.
            // FLAG_SUPPORTS_SEARCH allows users to search all documents the application
            // shares.
            add(
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                    DocumentsContract.Root.FLAG_SUPPORTS_RECENTS or
                    DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
            )

            // COLUMN_TITLE is the root title (e.g. Gallery, Drive).
            add(DocumentsContract.Root.COLUMN_TITLE, requireContext().getString(R.string.title))

            // This document id cannot change after it's shared.
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir))

            // The child MIME types are used to filter the roots and only present to the
            // user those roots that contain the desired type somewhere in their file hierarchy.
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, getChildMimeTypes(baseDir))
            add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDir.freeSpace)
            add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_cloud)
        }

        return result
    }
    // [END android_datastorage_document_provider_query_roots]

    // [START android_datastorage_document_provider_query_child_documents]
    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        return MatrixCursor(resolveDocumentProjection(projection)).apply {
            val parent: File = getFileForDocId(parentDocumentId)
            parent.listFiles()
                .forEach { file ->
                    includeFile(this, null, file)
                }
        }
    }
    // [END android_datastorage_document_provider_query_child_documents]

    // [START android_datastorage_document_provider_query_document]
    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        // Create a cursor with the requested projection, or the default projection.
        return MatrixCursor(resolveDocumentProjection(projection)).apply {
            includeFile(this, documentId, null)
        }
    }
    // [END android_datastorage_document_provider_query_document]

    // [START android_datastorage_document_provider_open_document_thumbnail]
    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val file = getThumbnailFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }
    // [END android_datastorage_document_provider_open_document_thumbnail]

    // [START android_datastorage_document_provider_open_document]
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        Log.v(TAG, "openDocument, mode: $mode")
        // It's OK to do network operations in this method to download the document,
        // as long as you periodically check the CancellationSignal. If you have an
        // extremely large file to transfer from the network, a better solution may
        // be pipes or sockets (see ParcelFileDescriptor for helper methods).

        val file: File = getFileForDocId(documentId)
        val accessMode: Int = ParcelFileDescriptor.parseMode(mode)

        val isWrite: Boolean = mode.contains("w")
        return if (isWrite) {
            val handler = Handler(requireContext().mainLooper)
            // Attach a close listener if the document is opened in write mode.
            try {
                ParcelFileDescriptor.open(file, accessMode, handler) {
                    // Update the file with the cloud server. The client is done writing.
                    Log.i(TAG, "A file with id $documentId has been closed! Time to update the server.")
                }
            } catch (e: IOException) {
                throw FileNotFoundException(
                    "Failed to open document with id $documentId and mode $mode"
                )
            }
        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }
    // [END android_datastorage_document_provider_open_document]

    // [START android_datastorage_document_provider_query_recent_documents]
    override fun queryRecentDocuments(rootId: String?, projection: Array<out String>?): Cursor {
        // This example implementation walks a
        // local file structure to find the most recently
        // modified files.  Other implementations might
        // include making a network call to query a
        // server.

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))

        val parent: File = getFileForDocId(rootId)

        // Create a queue to store the most recent documents,
        // which orders by last modified.
        val lastModifiedFiles = PriorityQueue(
            5,
            Comparator<File> { i, j ->
                i.lastModified().compareTo(j.lastModified())
            }
        )

        // Iterate through all files and directories
        // in the file structure under the root.  If
        // the file is more recent than the least
        // recently modified, add it to the queue,
        // limiting the number of results.
        val pending: MutableList<File> = mutableListOf()

        // Start by adding the parent to the list of files to be processed.
        pending.add(parent)

        // Do while we still have unexamined files.
        while (pending.isNotEmpty()) {
            // Take a file from the list of unprocessed files.
            val file: File = pending.removeAt(0)
            if (file.isDirectory) {
                // If it's a directory, add all its children to the unprocessed list.
                pending += file.listFiles()
            } else {
                // If it's a file, add it to the ordered queue.
                lastModifiedFiles.add(file)
            }
        }

        // Add the most recent files to the cursor,
        // not exceeding the max number of results.
        for (i in 0 until Math.min(MAX_LAST_MODIFIED + 1, lastModifiedFiles.size)) {
            val file: File = lastModifiedFiles.remove()
            includeFile(result, null, file)
        }
        return result
    }
    // [END android_datastorage_document_provider_query_recent_documents]

    // [START android_datastorage_document_provider_create_document]
    override fun createDocument(documentId: String?, mimeType: String?, displayName: String?): String {
        val parent: File = getFileForDocId(documentId)
        val file: File = try {
            File(parent.path, displayName).apply {
                createNewFile()
                setWritable(true)
                setReadable(true)
            }
        } catch (e: IOException) {
            throw FileNotFoundException(
                "Failed to create document with name $displayName and documentId $documentId"
            )
        }

        return getDocIdForFile(file)
    }
    // [END android_datastorage_document_provider_create_document]

    // [START android_datastorage_document_provider_virtual_files]
    var SUPPORTED_MIME_TYPES: Array<String> = arrayOf("image/png", "image/jpg")
    override fun openTypedDocument(
        documentId: String?,
        mimeTypeFilter: String,
        opts: Bundle?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        return try {
            // Determine which supported MIME type the client app requested.
            when (mimeTypeFilter) {
                "image/jpg" -> openJpgDocument(documentId)
                "image/png", "image/*", "*/*" -> openPngDocument(documentId)
                else -> throw IllegalArgumentException("Invalid mimeTypeFilter $mimeTypeFilter")
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.message.orEmpty())
            null
        }
    }

    override fun getDocumentStreamTypes(documentId: String, mimeTypeFilter: String): Array<String> {
        return when (mimeTypeFilter) {
            "*/*", "image/*" -> {
                // Return all supported MIME types if the client app
                // passes in '*/*' or 'image/*'.
                SUPPORTED_MIME_TYPES
            }
            else -> {
                // Filter the list of supported mime types to find a match.
                SUPPORTED_MIME_TYPES.filter { it == mimeTypeFilter }.toTypedArray()
            }
        }
    }
    // [END android_datastorage_document_provider_virtual_files]

    private val baseDir: File
        get() = requireContext().filesDir

    private fun getFileForDocId(docId: String?): File = File(baseDir, docId.orEmpty())

    private fun getThumbnailFileForDocId(docId: String?): File = getFileForDocId(docId)

    private fun getDocIdForFile(file: File): String = file.absolutePath

    private fun getChildMimeTypes(parent: File): String =
        if (parent.isDirectory) "image/*\ntext/*" else "*/*"

    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        val target = file ?: getFileForDocId(docId)
        result.newRow().add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getDocIdForFile(target))
    }

    private fun openJpgDocument(documentId: String?): AssetFileDescriptor =
        AssetFileDescriptor(
            ParcelFileDescriptor.open(getFileForDocId(documentId), ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            AssetFileDescriptor.UNKNOWN_LENGTH,
        )

    private fun openPngDocument(documentId: String?): AssetFileDescriptor =
        AssetFileDescriptor(
            ParcelFileDescriptor.open(getFileForDocId(documentId), ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            AssetFileDescriptor.UNKNOWN_LENGTH,
        )
}

private class AuthenticatedCloudProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    // [START android_datastorage_document_provider_security_query_roots]
    override fun queryRoots(projection: Array<out String>): Cursor {
        // [START_EXCLUDE]
        val result = MatrixCursor(resolveRootProjection(projection))
        // [END_EXCLUDE]
        // If user is not logged in, return an empty root cursor.  This removes our
        // provider from the list entirely.
        if (!isUserLoggedIn()) {
            return result
        }
        // [END android_datastorage_document_provider_security_query_roots]
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(resolveDocumentProjection(projection))

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor =
        MatrixCursor(resolveDocumentProjection(projection))

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor =
        ParcelFileDescriptor.open(File(requireContext().filesDir, documentId.orEmpty()), ParcelFileDescriptor.MODE_READ_ONLY)
}

private class CloudProviderLoginActivity : ComponentActivity() {

    // [START android_datastorage_document_provider_login]
    private fun onLoginButtonClick() {
        loginOrLogout()
        getContentResolver().notifyChange(
            DocumentsContract.buildRootsUri(AUTHORITY),
            null
        )
    }
    // [END android_datastorage_document_provider_login]

    private fun loginOrLogout() {}
}

private const val TAG = "MyCloudProvider"
private const val ROOT = "root"
private const val AUTHORITY = "com.example.android.storageprovider.documents"
private const val MAX_LAST_MODIFIED = 5

private object BuildConfig {
    const val DOCUMENTS_AUTHORITY = AUTHORITY
}

private fun resolveRootProjection(projection: Array<out String>?): Array<out String> =
    projection ?: DEFAULT_ROOT_PROJECTION

private fun resolveDocumentProjection(projection: Array<out String>?): Array<out String> =
    projection ?: DEFAULT_DOCUMENT_PROJECTION

private fun isUserLoggedIn(): Boolean = false
