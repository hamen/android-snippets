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

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.util.SparseIntArray
import androidx.core.util.size
import androidx.fragment.app.Fragment
import com.example.snippets.datastorage.R
import java.io.FileOutputStream
import java.io.IOException

class PrintCustomDocumentFragment : Fragment() {
    // [START android_datastorage_printing_custom_start]
    private fun doPrint() {
        activity?.also { context ->
            // Get a PrintManager instance.
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            // Set job name, which will be displayed in the print queue.
            val jobName = "${context.getString(R.string.app_name)} Document"
            // Start a print job, passing in a PrintDocumentAdapter implementation
            // to handle the generation of a print document.
            printManager.print(jobName, MyPrintDocumentAdapter(context), null)
        }
    }
    // [END android_datastorage_printing_custom_start]
}

private class MyPrintDocumentAdapter(private val activity: Context) : PrintDocumentAdapter() {
    private var pdfDocument: PrintedPdfDocument? = null
    private var totalPages = 0
    private val writtenPagesArray = SparseIntArray()

    // [START android_datastorage_printing_custom_on_layout]
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        // Create a new PdfDocument with the requested page attributes.
        pdfDocument = PrintedPdfDocument(activity, newAttributes)

        // Respond to cancellation request.
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        // Compute the expected number of printed pages.
        val pages = computePageCount(newAttributes)

        if (pages > 0) {
            // Return print information to print framework.
            PrintDocumentInfo.Builder("print_output.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pages)
                .build()
                .also { info ->
                    // Content layout reflow is complete.
                    callback.onLayoutFinished(info, true)
                }
        } else {
            // Otherwise report an error to the print framework.
            callback.onLayoutFailed("Page count calculation failed.")
        }
    }
    // [END android_datastorage_printing_custom_on_layout]

    // [START android_datastorage_printing_custom_page_count]
    private fun computePageCount(printAttributes: PrintAttributes): Int {
        var itemsPerPage = 4 // default item count for portrait mode.

        val pageSize = printAttributes.mediaSize
        if (pageSize?.isPortrait == false) {
            // Six items per page in landscape orientation.
            itemsPerPage = 6
        }

        // Determine number of print items.
        val printItemCount: Int = getPrintItemCount()

        return Math.ceil((printItemCount / itemsPerPage.toDouble())).toInt()
    }
    // [END android_datastorage_printing_custom_page_count]

    // [START android_datastorage_printing_custom_on_write]
    override fun onWrite(
        pageRanges: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        // Iterate over each page of the document,
        // check if it's in the output range.
        for (i in 0 until totalPages) {
            // Check to see if this page is in the output range.
            if (containsPage(pageRanges, i)) {
                // If so, add it to writtenPagesArray. writtenPagesArray.size()
                // is used to compute the next output page index.
                writtenPagesArray.append(writtenPagesArray.size(), i)
                pdfDocument?.startPage(i)?.also { page ->

                    // check for cancellation.
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        pdfDocument?.close()
                        pdfDocument = null
                        return
                    }

                    // Draw page content for printing.
                    drawPage(page)

                    // Rendering is complete, so page can be finalized.
                    pdfDocument?.finishPage(page)
                }
            }
        }

        // Write PDF document to file.
        try {
            pdfDocument?.writeTo(FileOutputStream(destination.fileDescriptor))
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
            return
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
        val writtenPages = computeWrittenPages()
        // Signal the print framework the document is complete.
        callback.onWriteFinished(writtenPages)

        // ...
    }
    // [END android_datastorage_printing_custom_on_write]

    // [START android_datastorage_printing_custom_draw_page]
    private fun drawPage(page: PdfDocument.Page) {
        page.canvas.apply {

            // units are in points (1/72 of an inch).
            val titleBaseLine = 72f
            val leftMargin = 54f

            val paint = Paint()
            paint.color = Color.BLACK
            paint.textSize = 36f
            drawText("Test Title", leftMargin, titleBaseLine, paint)

            paint.textSize = 11f
            drawText("Test paragraph", leftMargin, titleBaseLine + 25, paint)

            paint.color = Color.BLUE
            drawRect(100f, 100f, 172f, 172f, paint)
        }
    }
    // [END android_datastorage_printing_custom_draw_page]

    private fun getPrintItemCount(): Int = 12

    private fun containsPage(pageRanges: Array<out PageRange>, page: Int): Boolean =
        pageRanges.any { page in it.start..it.end }

    private fun computeWrittenPages(): Array<PageRange> =
        Array(writtenPagesArray.size) { index ->
            val page = writtenPagesArray.valueAt(index)
            PageRange(page, page)
        }
}
