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
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.snippets.datastorage.R

private const val TAG = "PrintingHtmlDocs"

class PrintHtmlFragment : Fragment() {
    private val printJobs = mutableListOf<PrintJob>()

    // [START android_datastorage_printing_html_webview]
    private var mWebView: WebView? = null

    private fun doWebViewPrint() {
        // Create a WebView object specifically for printing.
        val webView = WebView(requireActivity())
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                Log.i(TAG, "page finished loading $url")
                createWebPrintJob(view)
                mWebView = null
            }
        }

        // Generate an HTML document on the fly:
        val htmlDocument =
            "<html><body><h1>Test Content</h1><p>Testing, testing, testing...</p></body></html>"
        webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null)

        // Keep a reference to WebView object until you pass the PrintDocumentAdapter
        // to the PrintManager.
        mWebView = webView
    }
    // [END android_datastorage_printing_html_webview]

    // [START android_datastorage_printing_html_print_job]
    private fun createWebPrintJob(webView: WebView) {

        // Get a PrintManager instance.
        (activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.let { printManager ->

            val jobName = "${getString(R.string.app_name)} Document"

            // Get a print adapter instance.
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            // Create a print job with name and adapter instance.
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            ).also { printJob ->

                // Save the job object for later status checking.
                printJobs += printJob
            }
        }
    }
    // [END android_datastorage_printing_html_print_job]
}

private fun loadHtmlWithAssets(webView: WebView, htmlBody: String) {
    // [START android_datastorage_printing_html_assets]
    webView.loadDataWithBaseURL(
        "file:///android_asset/images/",
        htmlBody,
        "text/HTML",
        "UTF-8",
        null
    )
    // [END android_datastorage_printing_html_assets]
}

private fun loadWebPage(webView: WebView) {
    // [START android_datastorage_printing_html_load_url]
    webView.loadUrl("https://developer.android.com/about/index.html")
    // [END android_datastorage_printing_html_load_url]
}
