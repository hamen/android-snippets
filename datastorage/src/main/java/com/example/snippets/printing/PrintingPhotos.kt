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

package com.example.snippets.printing

import android.graphics.BitmapFactory
import androidx.fragment.app.Fragment
import androidx.print.PrintHelper
import com.example.snippets.datastorage.R

class PrintPhotoFragment : Fragment() {
    // [START android_datastorage_printing_photo]
    private fun doPhotoPrint() {
        activity?.also { context ->
            PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }.also { printHelper ->
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.cupcake)
                printHelper.printBitmap("droids.jpg - test print", bitmap)
            }
        }
    }
    // [END android_datastorage_printing_photo]
}
