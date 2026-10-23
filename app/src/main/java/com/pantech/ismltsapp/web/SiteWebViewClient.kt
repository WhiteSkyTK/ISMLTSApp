package com.pantech.ismltsapp.web

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Keeps the app on the ISMLTS site: its pages load in the app, other https links open in the browser (or the right
 * app for mail, phone and calendar links), and anything else is ignored. Certificate errors are never skipped
 * (onReceivedSslError is left to WebView, which cancels the page).
 */
class SiteWebViewClient(
    private val links: SiteLinks,
    private val listener: Listener
) : WebViewClient() {

    interface Listener {
        fun onPageLoading()
        fun onPageLoaded()
        fun onPageFailed()
        fun openExternally(uri: Uri)
        fun onWebViewCrashed()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        when (links.route(request.url.toString())) {
            LinkAction.IN_APP -> false // WebView loads it itself, keeping form posts and redirects intact
            LinkAction.EXTERNAL -> {
                listener.openExternally(request.url)
                true
            }
            LinkAction.BLOCKED -> true
        }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        listener.onPageLoading()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        listener.onPageLoaded()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) listener.onPageFailed()
    }

    // The page's renderer crashed or was stopped to save memory: let the activity rebuild rather than crash the app
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        listener.onWebViewCrashed()
        return true
    }
}
