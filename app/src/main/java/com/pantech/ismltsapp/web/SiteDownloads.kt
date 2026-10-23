package com.pantech.ismltsapp.web

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.net.toUri
import com.pantech.ismltsapp.R

/**
 * Files the site sends as downloads (for example a submitted file when the site keeps files itself) go to the
 * phone's download manager. The sign-in cookie is only ever sent back to the ISMLTS site; a download from anywhere
 * else is handed to the browser instead.
 */
class SiteDownloads(
    private val context: Context,
    private val links: SiteLinks,
    private val openExternally: (Uri) -> Unit
) : DownloadListener {

    override fun onDownloadStart(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        val uri = url.toUri()
        if (!links.isOnSite(url)) {
            if (links.route(url) == LinkAction.EXTERNAL) openExternally(uri)
            return
        }

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(uri).apply {
            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            userAgent?.let { addRequestHeader("User-Agent", it) }
            mimeType?.let { setMimeType(it) }
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Android 10+ lets the download manager write to Downloads without a storage permission;
            // older phones keep the file in the app's own folder (opened from the notification)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            } else {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            }
        }
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        Toast.makeText(context, context.getString(R.string.download_started, fileName), Toast.LENGTH_SHORT).show()
    }
}
