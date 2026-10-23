package com.pantech.ismltsapp.web

import java.net.URI
import java.net.URISyntaxException

/** What the app does with a link the website (or another app) asks it to open. */
enum class LinkAction {
    /** A page of the ISMLTS site: load it in the app. */
    IN_APP,

    /** Somewhere else (Azure Storage downloads, the IIE Library, email, a calendar feed): hand it to another app. */
    EXTERNAL,

    /** Anything that could reach into the phone (intent:, file:, javascript:, content:, data:): ignore it. */
    BLOCKED
}

/**
 * The rules for which links belong to the ISMLTS site. Plain Kotlin (java.net.URI, no Android classes), so the
 * rules are unit-tested on the build machine.
 */
class SiteLinks(baseUrl: String) {

    private val base: URI = URI(baseUrl.trim().trimEnd('/') + "/")

    init {
        require(base.scheme == "https" || base.scheme == "http") { "The site address must start with https://" }
        require(!base.host.isNullOrEmpty()) { "The site address needs a host name" }
    }

    val host: String = base.host

    /** The full address of a page on the site, for example url("Calendar"). */
    fun url(path: String): String = base.resolve(path.trimStart('/')).toString()

    /** Same scheme, host and port as the site: never a look-alike such as ismlts.example.evil.com. */
    fun isOnSite(url: String?): Boolean {
        val uri = parse(url) ?: return false
        return uri.scheme.equals(base.scheme, ignoreCase = true) &&
            uri.host.equals(base.host, ignoreCase = true) &&
            effectivePort(uri) == effectivePort(base)
    }

    fun route(url: String?): LinkAction {
        val uri = parse(url) ?: return LinkAction.BLOCKED
        return when {
            isOnSite(url) -> LinkAction.IN_APP
            uri.scheme?.lowercase() in EXTERNAL_SCHEMES -> LinkAction.EXTERNAL
            else -> LinkAction.BLOCKED
        }
    }

    private fun parse(url: String?): URI? =
        if (url.isNullOrBlank()) null
        else try {
            URI(url.trim())
        } catch (_: URISyntaxException) {
            null
        }

    private fun effectivePort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    private companion object {
        val EXTERNAL_SCHEMES = setOf("https", "http", "mailto", "tel", "webcal")
    }
}
