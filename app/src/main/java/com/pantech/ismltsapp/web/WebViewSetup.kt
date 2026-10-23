package com.pantech.ismltsapp.web

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/** The WebView settings the ISMLTS site needs, and nothing more. */
object WebViewSetup {

    @SuppressLint("SetJavaScriptEnabled") // the site's forms, timers, QR scanning and location checks need JavaScript
    fun apply(webView: WebView, fontScale: Float, debuggable: Boolean) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)

            // The app only shows the site: no access to files on the phone, no http inside https pages,
            // and pages can't open new windows by themselves
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true

            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            // Follow the phone's text size setting, so students who need larger text get it
            textZoom = (fontScale * 100).toInt().coerceIn(85, 200)
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // The sign-in and device cookies are the site's own; nobody else's cookies are needed
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }

        // Chrome's remote debugging only in debug builds
        WebView.setWebContentsDebuggingEnabled(debuggable)
    }
}
