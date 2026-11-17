package com.raisoft.grabi.data.browser

import android.view.View
import android.net.Uri
import android.webkit.WebChromeClient
import com.raisoft.grabi.domain.model.DetectedMedia

/** Swappable browser engine contract; WebViewEngine is the Phase 5 implementation. */
interface BrowserEngine {
    val view: View
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun destroy()
    fun acceptFileChooser(uris: Array<Uri>?)

    interface Listener {
        fun onPageStarted(url: String)
        fun onProgress(progress: Int)
        fun onPageFinished(url: String, title: String)
        fun onTitle(title: String)
        fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean)
        fun onMediaDetected(media: DetectedMedia)
        fun onFileChooser(params: WebChromeClient.FileChooserParams)
        fun onDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?)
    }
}