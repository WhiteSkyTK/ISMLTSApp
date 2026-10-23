package com.pantech.ismltsapp.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteLinksTest {

    private val links = SiteLinks("https://ismlts.example.net/")

    @Test
    fun pagesOnTheSite_LoadInTheApp() {
        assertEquals(LinkAction.IN_APP, links.route("https://ismlts.example.net/Calendar"))
        assertEquals(LinkAction.IN_APP, links.route("https://ISMLTS.example.net/Attendance/Scan?code=AB12CD"))
        assertEquals(LinkAction.IN_APP, links.route("https://ismlts.example.net:443/Profile"))
    }

    @Test
    fun lookAlikesAndOtherSites_NeverLoadInTheApp() {
        assertEquals(LinkAction.EXTERNAL, links.route("https://ismlts.example.net.evil.com/Account/Login"))
        assertEquals(LinkAction.EXTERNAL, links.route("https://evil.com/?next=https://ismlts.example.net"))
        assertEquals(LinkAction.EXTERNAL, links.route("https://ismltsstore.blob.core.windows.net/submissions/1/a.pdf?sig=x"))
        // Same host over plain http, or on another port, isn't the site
        assertEquals(LinkAction.EXTERNAL, links.route("http://ismlts.example.net/Calendar"))
        assertEquals(LinkAction.EXTERNAL, links.route("https://ismlts.example.net:8443/Calendar"))
    }

    @Test
    fun mailPhoneAndCalendarLinks_GoToTheirApps() {
        assertEquals(LinkAction.EXTERNAL, links.route("mailto:admin@rcconnect.edu.za"))
        assertEquals(LinkAction.EXTERNAL, links.route("tel:+27110000000"))
        assertEquals(LinkAction.EXTERNAL, links.route("webcal://ismlts.example.net/Calendar/Feed?token=abc"))
    }

    @Test
    fun linksThatReachIntoThePhone_AreBlocked() {
        assertEquals(LinkAction.BLOCKED, links.route("intent://scan/#Intent;scheme=zxing;package=com.example;end"))
        assertEquals(LinkAction.BLOCKED, links.route("javascript:alert(1)"))
        assertEquals(LinkAction.BLOCKED, links.route("file:///data/data/com.pantech.ismltsapp/app_webview/Cookies"))
        assertEquals(LinkAction.BLOCKED, links.route("content://com.android.contacts/contacts"))
        assertEquals(LinkAction.BLOCKED, links.route("data:text/html,<script>alert(1)</script>"))
        assertEquals(LinkAction.BLOCKED, links.route(""))
        assertEquals(LinkAction.BLOCKED, links.route(null))
        assertEquals(LinkAction.BLOCKED, links.route("not a url"))
    }

    @Test
    fun locationIsOnlyForTheSitesOwnOrigin() {
        assertTrue(links.isOnSite("https://ismlts.example.net/"))
        assertTrue(links.isOnSite("https://ismlts.example.net"))
        assertFalse(links.isOnSite("https://evil.com/"))
        assertFalse(links.isOnSite("http://ismlts.example.net/"))
    }

    @Test
    fun pageAddressesAreBuiltFromTheSiteAddress() {
        assertEquals("https://ismlts.example.net/", links.url(""))
        assertEquals("https://ismlts.example.net/Insights/MyProgress", links.url("Insights/MyProgress"))
        assertEquals("https://ismlts.example.net/Calendar", links.url("/Calendar"))
        assertEquals("ismlts.example.net", links.host)
    }

    @Test
    fun aLocalTestSite_KeepsItsPort() {
        val local = SiteLinks("http://10.0.2.2:5269")

        assertEquals(LinkAction.IN_APP, local.route("http://10.0.2.2:5269/Calendar"))
        assertEquals(LinkAction.EXTERNAL, local.route("http://10.0.2.2:5000/Calendar"))
        assertEquals("http://10.0.2.2:5269/Profile", local.url("Profile"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aSiteAddressMustBeAWebAddress() {
        SiteLinks("ftp://ismlts.example.net")
    }
}
