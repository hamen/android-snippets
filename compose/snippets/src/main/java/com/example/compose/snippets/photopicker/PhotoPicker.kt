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

package com.example.compose.snippets.photopicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.MediaCapabilities
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable

@Composable
private fun PickSingleMediaCompose() {
    // [START android_compose_photopicker_pick_single_compose]
    // Registers a photo picker activity launcher in single-select mode.
    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    // Include only one of the following calls to launch(), depending on the types
    // of media that you want to let the user choose from.

    // Launch the photo picker and let the user choose images and videos.
    pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))

    // Launch the photo picker and let the user choose only images.
    pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))

    // Launch the photo picker and let the user choose only videos.
    pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))

    // Launch the photo picker and let the user choose only images/videos of a
    // specific MIME type, such as GIFs.
    val mimeType = "image/gif"
    pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.SingleMimeType(mimeType)))
    // [END android_compose_photopicker_pick_single_compose]
}

@Composable
private fun PickMultipleMediaCompose() {
    // [START android_compose_photopicker_pick_multiple_compose]
    // Registers a photo picker activity launcher in multi-select mode.
    // In this example, the app lets the user select up to 5 media files.
    val pickMultipleMedia =
        rememberLauncherForActivityResult(PickMultipleVisualMedia(5)) { uris ->
            // Callback is invoked after the user selects media items or closes the
            // photo picker.
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    // For this example, launch the photo picker and let the user choose images
    // and videos. If you want the user to select a specific type of media file,
    // use the overloaded versions of launch(), as shown in the section about how
    // to select a single media item.
    pickMultipleMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
    // [END android_compose_photopicker_pick_multiple_compose]
}

private fun persistMediaFileAccess(context: Context, uri: Uri) {
    // [START android_compose_photopicker_persist_access]
    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(uri, flag)
    // [END android_compose_photopicker_persist_access]
}

private class PhotoPickerTranscodingActivity : ComponentActivity() {
    // [START android_compose_photopicker_hdr_transcoding]
    // import androidx.activity.result.PickVisualMediaRequest
    // import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
    // import androidx.annotation.RequiresApi
    // import android.os.Build
    // import android.util.Log
    // import android.provider.MediaStore

    // Registers a photo picker activity launcher.
    val pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
        // Callback invoked after media selected or picker activity closed.
        if (uri != null) {
            Log.d("photo picker", "Selected URI: $uri")
        } else {
            Log.d("photo picker", "No media selected")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun launchPhotoPickerWithTranscodingSupport() {
        val mediaCapabilities = MediaCapabilities.Builder()
            .addSupportedHdrType(MediaCapabilities.TYPE_HLG10)
            .build()

        // Launch the photo picker and let the user choose only videos with
        // transcoding enabled.
        pickMedia.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.VideoOnly)
                .setMediaCapabilitiesForTranscoding(mediaCapabilities)
                .build()
        )
    }
    // [END android_compose_photopicker_hdr_transcoding]
}
