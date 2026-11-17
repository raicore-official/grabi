package com.raisoft.grabi.data.browser

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import com.raisoft.grabi.domain.MediaSniffer
import com.raisoft.grabi.domain.model.DetectedMedia
import java.io.ByteArrayInputStream

class WebViewEngine(
    context: Context,
    incognito: Boolean,
    private val adblock: AdblockEngine,
    private val listener: BrowserEngine.Listener,
) : BrowserEngine {

    private var currentPageUrl = ""

    override val view: WebView = WebView(context)

    init {
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !incognito
            databaseEnabled = !incognito
            @Suppress("DEPRECATION")
            saveFormData = !incognito
            cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                val url = request.url.toString()
                if (adblock.shouldBlock(url, currentPageUrl)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                MediaSniffer.classify(url, null)?.let { kind ->
                    listener.onMediaDetected(DetectedMedia(url, kind, null, null))
                }
                return null
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                currentPageUrl = url
                listener.onPageStarted(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                currentPageUrl = url
                listener.onPageFinished(url, view.title.orEmpty())
                listener.onNavigationStateChanged(view.canGoBack(), view.canGoForward())
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                listener.onNavigationStateChanged(view.canGoBack(), view.canGoForward())
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                listener.onProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                listener.onTitle(title.orEmpty())
            }
        }
    }

    fun applyTheme(dark: Boolean, backgroundArgb: Int) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, dark)
        view.setBackgroundColor(backgroundArgb)
    }

    override fun loadUrl(url: String) {
        view.loadUrl(url)
    }

    override fun goBack() {
        if (view.canGoBack()) view.goBack()
    }

    override fun goForward() {
        if (view.canGoForward()) view.goForward()
    }

    override fun reload() {
        view.reload()
    }

    override fun destroy() {
        view.destroy()
    }
}