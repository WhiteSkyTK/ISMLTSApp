package com.pantech.ismltsapp.web

import android.content.Intent
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * The parts of the site that need the phone: location (checked when a student scans in to a class), the file
 * picker (handing in work) and page progress.
 */
class SiteChromeClient(
    private val links: SiteLinks,
    private val host: Host
) : WebChromeClient() {

    interface Host {
        fun onProgress(percent: Int)

        /** Ask for location permission if needed, then report whether the page may have it. */
        fun requestLocation(onResult: (Boolean) -> Unit)

        /** Open Android's file picker; false if it couldn't be opened. */
        fun pickFiles(pickerIntent: Intent, callback: ValueCallback<Array<Uri>>): Boolean
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        host.onProgress(newProgress)
    }

    // Only the ISMLTS site ever gets the phone's location, and it isn't remembered for other pages
    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        if (!links.isOnSite(origin)) {
            callback.invoke(origin, false, false)
            return
        }
        host.requestLocation { granted -> callback.invoke(origin, granted, false) }
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean = host.pickFiles(fileChooserParams.createIntent(), filePathCallback)
}
