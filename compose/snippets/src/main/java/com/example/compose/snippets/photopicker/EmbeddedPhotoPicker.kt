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

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.photopicker.embedded.compose.EmbeddedPhotoPicker
import androidx.photopicker.embedded.compose.EmbeddedPhotoPickerState
import androidx.photopicker.embedded.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.embedded.compose.rememberEmbeddedPhotoPickerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val _attachments = MutableStateFlow(emptyList<Uri>())

@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun EmbeddedPhotoPickerBasic() {
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15) {
        // [START android_compose_photopicker_embedded_basic]
        val coroutineScope = rememberCoroutineScope()
        val pickerState = rememberEmbeddedPhotoPickerState(
            onUriPermissionGranted = { uris ->
                _attachments.value += uris
            },
            onUriPermissionRevoked = { uris ->
                _attachments.value -= uris
            },
            onSelectionComplete = {
                // Hide the embedded photo picker as the user is done with the
                // photo/video selection.
            },
        )

        EmbeddedPhotoPicker(
            state = pickerState,
        )
        // [END android_compose_photopicker_embedded_basic]
    }
}

@OptIn(ExperimentalPhotoPickerComposeApi::class)
private fun deselectMedia(
    coroutineScope: CoroutineScope,
    pickerState: EmbeddedPhotoPickerState,
    uris: List<Uri>,
) {
    // [START android_compose_photopicker_embedded_deselect]
    coroutineScope.launch {
        // Signal unselected media to the picker.
        pickerState.deselectUris(uris)
        // Remove them from the list of selected media to be reflected in the app's UI.
        _attachments.value -= uris
    }
    // [END android_compose_photopicker_embedded_deselect]
}

@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun EmbeddedPhotoPickerAccentColor() {
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15) {
        // [START android_compose_photopicker_embedded_accent_color]
        val info = EmbeddedPhotoPickerFeatureInfo.Builder().setAccentColor(0xFF0000).build()

        EmbeddedPhotoPicker(
            embeddedPhotoPickerFeatureInfo = info,
            // [START_EXCLUDE]
            state = rememberEmbeddedPhotoPickerState(),
            // [END_EXCLUDE]
        )
        // [END android_compose_photopicker_embedded_accent_color]
    }
}

@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun EmbeddedPhotoPickerDimensions() {
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15) {
        // [START android_compose_photopicker_embedded_dimensions]
        EmbeddedPhotoPicker(
            modifier = Modifier.height(500.dp),
            // [START_EXCLUDE]
            state = rememberEmbeddedPhotoPickerState(),
            // [END_EXCLUDE]
        )
        // [END android_compose_photopicker_embedded_dimensions]
    }
}
