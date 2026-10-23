package com.pantech.ismltsapp.config

import com.pantech.ismltsapp.BuildConfig
import com.pantech.ismltsapp.web.SiteLinks

/**
 * The ISMLTS site this build talks to. The address comes from the Gradle property `ismlts.siteUrl`
 * (see README), so pointing a test build at another site never means editing code.
 */
object SiteConfig {
    val links: SiteLinks = SiteLinks(BuildConfig.SITE_URL)

    val homeUrl: String get() = links.url("")

    fun url(destination: Destination): String = links.url(destination.path)
}
