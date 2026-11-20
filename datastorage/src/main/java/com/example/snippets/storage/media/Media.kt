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

package com.example.snippets.storage.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import java.util.concurrent.TimeUnit

private fun queryMediaStoreGeneral(
    applicationContext: Context,
    mediaDatabaseColumnsToRetrieve: String,
    sqlWhereClauseWithPlaceholderVariables: String,
    valuesOfPlaceholderVariables: Array<String>,
    sqlOrderByClause: String,
) {
    // [START android_datastorage_media_query_general]
    val projection = arrayOf(mediaDatabaseColumnsToRetrieve)
    val selection = sqlWhereClauseWithPlaceholderVariables
    val selectionArgs = valuesOfPlaceholderVariables
    val sortOrder = sqlOrderByClause

    applicationContext.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            // Use an ID column from the projection to get
            // a URI representing the media item itself.
        }
    }
    // [END android_datastorage_media_query_general]
}

private fun queryVideoCollection(contentResolver: ContentResolver) {
    // [START android_datastorage_media_query_collection]
    // Need the READ_EXTERNAL_STORAGE permission if accessing video files that your
    // app didn't create.

    // Container for information about each video.
    data class Video(
        val uri: Uri,
        val name: String,
        val duration: Int,
        val size: Int
    )
    val videoList = mutableListOf<Video>()

    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE
    )

    // Show only videos that are at least 5 minutes in duration.
    val selection = "${MediaStore.Video.Media.DURATION} >= ?"
    val selectionArgs = arrayOf(
        TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
    )

    // Display videos in alphabetical order based on their display name.
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    val query = contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )
    query?.use { cursor ->
        // Cache column indices.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val duration = cursor.getInt(durationColumn)
            val size = cursor.getInt(sizeColumn)

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id
            )

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            videoList += Video(contentUri, name, duration, size)
        }
    }
    // [END android_datastorage_media_query_collection]
}

private fun loadThumbnail(applicationContext: Context, contentUri: Uri) {
    // [START android_datastorage_media_load_thumbnail]
    // Load thumbnail of a specific media item.
    val thumbnail: Bitmap =
        applicationContext.contentResolver.loadThumbnail(
            contentUri, Size(640, 480), null
        )
    // [END android_datastorage_media_load_thumbnail]
}

private fun openMediaFileDescriptor(applicationContext: Context, contentUri: Uri) {
    // [START android_datastorage_media_open_file_descriptor]
    // Open a specific media item using ParcelFileDescriptor.
    val resolver = applicationContext.contentResolver

    // "rw" for read-and-write.
    // "rwt" for truncating or overwriting existing file contents.
    val readOnlyMode = "r"
    resolver.openFileDescriptor(contentUri, readOnlyMode).use { pfd ->
        // Perform operations on "pfd".
    }
    // [END android_datastorage_media_open_file_descriptor]
}

private fun openMediaFileStream(applicationContext: Context, contentUri: Uri) {
    // [START android_datastorage_media_open_file_stream]
    // Open a specific media item using InputStream.
    val resolver = applicationContext.contentResolver
    resolver.openInputStream(contentUri).use { stream ->
        // Perform operations on "stream".
    }
    // [END android_datastorage_media_open_file_stream]
}

private fun getStorageVolumes(context: Context) {
    // [START android_datastorage_media_storage_volumes]
    val volumeNames: Set<String> = MediaStore.getExternalVolumeNames(context)
    val firstVolumeName = volumeNames.iterator().next()
    // [END android_datastorage_media_storage_volumes]
}

private fun getPhotoLocation(
    contentResolver: ContentResolver,
    cursor: Cursor,
    idColumnIndex: Int,
) {
    // [START android_datastorage_media_photo_location]
    var photoUri: Uri = Uri.withAppendedPath(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        cursor.getString(idColumnIndex)
    )

    // Get location data using the Exifinterface library.
    // Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.
    photoUri = MediaStore.setRequireOriginal(photoUri)
    contentResolver.openInputStream(photoUri)?.use { stream ->
        ExifInterface(stream).run {
            // If lat/long is null, fall back to the coordinates (0, 0).
            val latLong = latLong ?: doubleArrayOf(0.0, 0.0)
        }
    }
    // [END android_datastorage_media_photo_location]
}

private fun addMediaItem(applicationContext: Context) {
    // [START android_datastorage_media_add_item]
    // Add a specific media item.
    val resolver = applicationContext.contentResolver

    // Find all audio files on the primary external storage device.
    val audioCollection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    // Publish a new song.
    val newSongDetails = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, "My Song.mp3")
    }

    // Keep a handle to the new song's URI in case you need to modify it
    // later.
    val myFavoriteSongUri = resolver
        .insert(audioCollection, newSongDetails)
    // [END android_datastorage_media_add_item]
}

private fun togglePendingStatus(applicationContext: Context) {
    // [START android_datastorage_media_toggle_pending]
    // Add a media item that other apps don't see until the item is
    // fully written to the media store.
    val resolver = applicationContext.contentResolver

    // Find all audio files on the primary external storage device.
    val audioCollection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    val songDetails = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, "My Workout Playlist.mp3")
        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }

    val songContentUri = resolver.insert(audioCollection, songDetails) ?: return

    // "w" for write.
    resolver.openFileDescriptor(songContentUri, "w", null).use { pfd ->
        // Write data into the pending audio file.
    }

    // Now that you're finished, release the "pending" status and let other apps
    // play the audio track.
    songDetails.clear()
    songDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
    resolver.update(songContentUri, songDetails, null, null)
    // [END android_datastorage_media_toggle_pending]
}

private fun updateMediaItem(
    applicationContext: Context,
    myFavoriteSongUri: Uri,
    mediaIdToUpdate: Long,
) {
    // [START android_datastorage_media_update_item]
    // Updates an existing media item.
    // [START_EXCLUDE silent]
    /*
    // [END_EXCLUDE]
    val mediaId = // MediaStore.Audio.Media._ID of item to update.
    // [START_EXCLUDE silent]
     */
    val mediaId = mediaIdToUpdate
    // [END_EXCLUDE]
    val resolver = applicationContext.contentResolver

    // When performing a single item update, prefer using the ID.
    val selection = "${MediaStore.Audio.Media._ID} = ?"

    // By using selection + args you protect against improper escaping of // values.
    val selectionArgs = arrayOf(mediaId.toString())

    // Update an existing song.
    val updatedSongDetails = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, "My Favorite Song.mp3")
    }

    // Use the individual song's URI to represent the collection that's
    // updated.
    val numSongsUpdated = resolver.update(
        myFavoriteSongUri,
        updatedSongDetails,
        selection,
        selectionArgs
    )
    // [END android_datastorage_media_update_item]
}

private fun updateInNativeCode(resolver: ContentResolver, cursor: Cursor) {
    // [START android_datastorage_media_update_native_code]
    val contentUri: Uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
    )
    val fileOpenMode = "r"
    val parcelFd = resolver.openFileDescriptor(contentUri, fileOpenMode)
    val fd = parcelFd?.detachFd()
    // Pass the integer value "fd" into your native code. Remember to call
    // close(2) on the file descriptor when you're done using it.
    // [END android_datastorage_media_update_native_code]
}

private fun removeMediaItem(applicationContext: Context) {
    // [START android_datastorage_media_remove_item]
    // Remove a specific media item.
    val resolver = applicationContext.contentResolver

    // URI of the image to remove.
    val imageUri = Uri.parse("...")

    // WHERE clause.
    val selection = "..."
    val selectionArgs = arrayOf("...")

    // Perform the actual removal.
    val numImagesRemoved = resolver.delete(
        imageUri,
        selection,
        selectionArgs
    )
    // [END android_datastorage_media_remove_item]
}
