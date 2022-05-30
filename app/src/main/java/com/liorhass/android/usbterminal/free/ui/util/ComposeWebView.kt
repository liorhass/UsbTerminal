// Copyright 2022 Lior Hass
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.liorhass.android.usbterminal.free.ui.util

import android.content.Intent
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.nio.charset.Charset

@Composable
fun ComposeWebView(
    url: String,
    onPageLoaded: ((url: String?) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Handling back button. From: https://stackoverflow.com/a/70784094/1071117
    var backEnabled by remember { mutableStateOf(false) }
    var webView: WebView? = null

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(context))
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Timber.d("shouldOverrideUrlLoading() URL='${request?.url}'")
                        request?.url ?: return false

                        // If our assetLoader recognizes the URL, we're handling it ourselves
                        if (assetLoader.shouldInterceptRequest(request.url) != null) {
                            return false
                        }

                        // Anything that we don't handle ourselves should be loaded by a browser
                        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        return true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        // Don't waste our time on trying to load a favicon.ico file
                        if (request.url.toString().lowercase().endsWith("/favicon.ico")) {
                            // Timber.d("shouldInterceptRequest() returning empty response for FAVICON URL='${request.url}'")
                            val inputStream = "".byteInputStream(Charset.defaultCharset())
                            return WebResourceResponse("text", "UTF-8", inputStream)
                        }
                        // Timber.d("shouldInterceptRequest() returning ${assetLoader.shouldInterceptRequest(request.url)}  URL='${request.url}'")
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(webView1: WebView?, url: String?) {
                        backEnabled = webView1?.canGoBack() ?: false
                        onPageLoaded?.invoke(url)
                    }
                }

                settings.allowFileAccess = false
                settings.allowContentAccess = false
//                setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Probably unnecessary
//                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                loadUrl(url)
            }
        },
        update = {
            // No need to load page here because our content is completely static, so a
            // load in the factory is enough
            // it.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            webView = it
            // Timber.d("update")
        }
    )
    BackHandler(enabled = backEnabled) {
        // Timber.d("BackHandler() going back'")
        webView?.goBack()
    }
}