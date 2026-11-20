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

package com.example.snippets.storage.datasets

import android.app.blob.BlobHandle
import android.app.blob.BlobStoreManager
import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

private fun Context.accessAndContributeSharedDataset(
    sha256DigestBytes: ByteArray,
    yourAppPackage: String,
    someOtherAppPackage: String,
    appCertificate: ByteArray,
    callback: Consumer<Int>,
) {
    // [START android_datastorage_datasets_open_blob]
    val blobStoreManager =
        getSystemService(BlobStoreManager::class.java) as BlobStoreManager
    // The label "Sample photos" is visible to the user.
    val blobHandle = BlobHandle.createWithSha256(
        sha256DigestBytes,
        "Sample photos",
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
        "photoTrainingDataset"
    )
    try {
        val input = ParcelFileDescriptor.AutoCloseInputStream(
            blobStoreManager.openBlob(blobHandle)
        )
        useDataset(input)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    // [END android_datastorage_datasets_open_blob]

    // [START android_datastorage_datasets_contribute_blob]
    val sessionId = blobStoreManager.createSession(blobHandle)
    try {
        val session = blobStoreManager.openSession(sessionId)
        try {
            // For this example, write 200 MiB at the beginning of the file.
            val output = ParcelFileDescriptor.AutoCloseOutputStream(
                session.openWrite(0, 1024 * 1024 * 200)
            )
            writeDataset(output)

            session.apply {
                allowSameSignatureAccess()
                allowPackageAccess(
                    yourAppPackage,
                    appCertificate
                )
                allowPackageAccess(
                    someOtherAppPackage,
                    appCertificate
                )
                commit(mainExecutor, callback)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    // [END android_datastorage_datasets_contribute_blob]
}

private fun useDataset(input: InputStream) {}

private fun writeDataset(output: OutputStream) {}
