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

package com.example.snippets.filesharing

import android.content.ContentResolver
import android.content.Intent

private fun retrieveMimeType(returnIntent: Intent, contentResolver: ContentResolver) {
    // [START android_datastorage_retrieve_info_mime_type]
    // ...
    /*
     * Get the file's content URI from the incoming Intent, then
     * get the file's MIME type.
     */
    val mimeType: String? = returnIntent.data?.let { returnUri ->
        contentResolver.getType(returnUri)
    }
    // ...
    // [END android_datastorage_retrieve_info_mime_type]
}
