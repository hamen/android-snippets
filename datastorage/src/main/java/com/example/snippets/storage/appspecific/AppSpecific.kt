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

package com.example.snippets.storage.appspecific

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageManager.ACTION_MANAGE_STORAGE
import android.util.Log
import androidx.core.content.getSystemService
import java.io.File
import java.util.UUID

private const val LOG_TAG = "AppSpecificStorage"

private fun accessInternalFile(context: Context, filename: String) {
    // [START android_datastorage_appspecific_internal_access_file]
    val file = File(context.filesDir, filename)
    // [END android_datastorage_appspecific_internal_access_file]
}

private fun storeFileUsingStream(context: Context) {
    // [START android_datastorage_appspecific_internal_store_stream]
    val filename = "myfile"
    val fileContents = "Hello world!"
    context.openFileOutput(filename, Context.MODE_PRIVATE).use {
        it.write(fileContents.toByteArray())
    }
    // [END android_datastorage_appspecific_internal_store_stream]
}

private fun accessFileUsingStream(context: Context, filename: String) {
    // [START android_datastorage_appspecific_internal_access_stream]
    context.openFileInput(filename).bufferedReader().useLines { lines ->
        lines.fold("") { some, text ->
            "$some\n$text"
        }
    }
    // [END android_datastorage_appspecific_internal_access_stream]
}

private fun viewInternalFileList(context: Context) {
    // [START android_datastorage_appspecific_internal_file_list]
    var files: Array<String> = context.fileList()
    // [END android_datastorage_appspecific_internal_file_list]
}

private fun createNestedDirectory(context: Context, dirName: String) {
    // [START android_datastorage_appspecific_internal_nested_dir]
    context.getDir(dirName, Context.MODE_PRIVATE)
    // [END android_datastorage_appspecific_internal_nested_dir]
}

private fun manageInternalCache(context: Context, filename: String, cacheFileName: String) {
    // [START android_datastorage_appspecific_internal_create_cache]
    File.createTempFile(filename, null, context.cacheDir)
    // [END android_datastorage_appspecific_internal_create_cache]

    // [START android_datastorage_appspecific_internal_access_cache]
    val cacheFile = File(context.cacheDir, filename)
    // [END android_datastorage_appspecific_internal_access_cache]

    // [START android_datastorage_appspecific_internal_delete_cache_file]
    cacheFile.delete()
    // [END android_datastorage_appspecific_internal_delete_cache_file]

    // [START android_datastorage_appspecific_internal_delete_file]
    context.deleteFile(cacheFileName)
    // [END android_datastorage_appspecific_internal_delete_file]
}

// [START android_datastorage_appspecific_external_verify_availability]
// Checks if a volume containing external storage is available
// for read and write.
fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

// Checks if a volume containing external storage is available to at least read.
fun isExternalStorageReadable(): Boolean {
    return Environment.getExternalStorageState() in
        setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}
// [END android_datastorage_appspecific_external_verify_availability]

private fun manageExternalFilesAndCache(context: Context, filename: String) {
    // [START android_datastorage_appspecific_external_access_file]
    val appSpecificExternalDir = File(context.getExternalFilesDir(null), filename)
    // [END android_datastorage_appspecific_external_access_file]

    // [START android_datastorage_appspecific_external_create_cache]
    val externalCacheFile = File(context.externalCacheDir, filename)
    // [END android_datastorage_appspecific_external_create_cache]

    // [START android_datastorage_appspecific_external_remove_cache]
    externalCacheFile.delete()
    // [END android_datastorage_appspecific_external_remove_cache]
}

// [START android_datastorage_appspecific_external_media_dir]
fun getAppSpecificAlbumStorageDir(context: Context, albumName: String): File? {
    // Get the pictures directory that's inside the app-specific directory on
    // external storage.
    val file = File(
        context.getExternalFilesDir(
            Environment.DIRECTORY_PICTURES
        ),
        albumName
    )
    if (!file.mkdirs()) {
        Log.e(LOG_TAG, "Directory not created")
    }
    return file
}
// [END android_datastorage_appspecific_external_media_dir]

private fun Context.queryFreeSpace() {
    // [START android_datastorage_appspecific_query_free_space]
    // App needs 10 MB within internal storage.
    val NUM_BYTES_NEEDED_FOR_MY_APP = 1024 * 1024 * 10L

    val storageManager = applicationContext.getSystemService<StorageManager>()!!
    val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(filesDir)
    val availableBytes: Long =
        storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
    if (availableBytes >= NUM_BYTES_NEEDED_FOR_MY_APP) {
        storageManager.allocateBytes(
            appSpecificInternalDirUuid, NUM_BYTES_NEEDED_FOR_MY_APP
        )
    } else {
        val storageIntent = Intent().apply {
            // To request that the user remove all app cache files instead, set
            // "action" to ACTION_CLEAR_APP_CACHE.
            action = ACTION_MANAGE_STORAGE
        }
    }
    // [END android_datastorage_appspecific_query_free_space]
}
