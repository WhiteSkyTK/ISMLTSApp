package com.pantech.ismltsapp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pantech.ismltsapp.config.Destination
import com.pantech.ismltsapp.config.SiteConfig
import com.pantech.ismltsapp.web.SiteChromeClient
import com.pantech.ismltsapp.web.SiteDownloads
import com.pantech.ismltsapp.web.SiteWebViewClient
import com.pantech.ismltsapp.web.WebViewSetup

/**
 * The app's one screen: a home page of tiles, and the ISMLTS website in a WebView for everything else.
 * Link rules live in SiteLinks, WebView settings in WebViewSetup, and the page, location, file and download
 * handling in the classes under web/; this activity only wires them to the views.
 */
class MainActivity : AppCompatActivity(), SiteWebViewClient.Listener, SiteChromeClient.Host {

    private val links = SiteConfig.links

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var splashScreen: View
    private lateinit var errorScreen: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var headerTitle: TextView
    private lateinit var homeGridContainer: View
    private lateinit var webViewContainer: View

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var locationResult: ((Boolean) -> Unit)? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        locationResult?.invoke(grants.values.any { it })
        locationResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        splashScreen = findViewById(R.id.splashScreen)
        errorScreen = findViewById(R.id.errorScreen)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNav = findViewById(R.id.bottomNav)
        headerTitle = findViewById(R.id.headerTitle)
        homeGridContainer = findViewById(R.id.homeGridContainer)
        webViewContainer = findViewById(R.id.webViewContainer)

        WebViewSetup.apply(webView, resources.configuration.fontScale, BuildConfig.DEBUG)
        webView.webViewClient = SiteWebViewClient(links, this)
        webView.webChromeClient = SiteChromeClient(links, this)
        webView.setDownloadListener(SiteDownloads(this, links, ::openExternally))

        setupTiles()
        setupBottomNavigation()
        setupRefresh()
        setupBackButton()
        findViewById<ImageView>(R.id.profileButton).setOnClickListener { open(Destination.PROFILE) }
        findViewById<Button>(R.id.retryButton).setOnClickListener {
            errorScreen.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            webView.reload()
        }

        if (!openLinkFrom(intent)) showHomeGrid()
        Handler(Looper.getMainLooper()).postDelayed({ hideSplashScreen() }, SPLASH_MILLIS)
    }

    // A register QR code (or another ISMLTS link) opened while the app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openLinkFrom(intent)
    }

    override fun onPause() {
        super.onPause()
        // Keeps the sign-in and device cookies, so the one-phone-per-student check survives the app closing
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    // ---------- Navigation ----------

    private fun setupTiles() {
        val tiles = mapOf(
            R.id.tileScanIn to Destination.SCAN_IN,
            R.id.tileWork to Destination.WORK_TO_DO,
            R.id.tileAnnouncements to Destination.ANNOUNCEMENTS,
            R.id.tileProgress to Destination.PROGRESS,
            R.id.tileCalendar to Destination.CALENDAR,
            R.id.tileProfile to Destination.PROFILE
        )
        for ((id, destination) in tiles) {
            findViewById<View>(id).setOnClickListener { open(destination) }
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_home) {
                showHomeGrid()
                true
            } else {
                Destination.forNavItem(item.itemId)?.let { open(it) } != null
            }
        }
    }

    private fun open(destination: Destination) =
        showWebViewPage(SiteConfig.url(destination), getString(destination.title), destination.navItem)

    // Only the ISMLTS site's own links open in the app; anything else in a VIEW intent is ignored
    private fun openLinkFrom(intent: Intent?): Boolean {
        val link = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data?.toString() ?: return false
        if (!links.isOnSite(link)) return false
        val title = if (link.toUri().path?.startsWith("/${Destination.SCAN_IN.path}", ignoreCase = true) == true) {
            getString(Destination.SCAN_IN.title)
        } else {
            getString(R.string.app_name)
        }
        showWebViewPage(link, title, null)
        return true
    }

    private fun showHomeGrid() {
        homeGridContainer.visibility = View.VISIBLE
        webViewContainer.visibility = View.GONE
        errorScreen.visibility = View.GONE
        progressBar.visibility = View.GONE
        headerTitle.text = getString(R.string.nav_home)
        selectNavItem(R.id.nav_home)
    }

    private fun showWebViewPage(url: String, title: String, navItemId: Int?) {
        homeGridContainer.visibility = View.GONE
        webViewContainer.visibility = View.VISIBLE
        errorScreen.visibility = View.GONE
        headerTitle.text = title
        selectNavItem(navItemId)
        if (webView.url != url) {
            progressBar.visibility = View.VISIBLE
            webView.loadUrl(url)
        }
    }

    private fun selectNavItem(itemId: Int?) {
        bottomNav.menu.forEach { it.isChecked = it.itemId == itemId }
    }

    private fun setupRefresh() {
        swipeRefresh.setOnRefreshListener { webView.reload() }
        swipeRefresh.setColorSchemeResources(R.color.primary_blue)
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webViewContainer.isVisible && webView.canGoBack() -> webView.goBack()
                    webViewContainer.isVisible -> showHomeGrid()
                    else -> finish()
                }
            }
        })
    }

    private fun hideSplashScreen() {
        if (splashScreen.visibility != View.GONE) {
            splashScreen.animate().alpha(0f).setDuration(SPLASH_FADE_MILLIS)
                .withEndAction { splashScreen.visibility = View.GONE }.start()
        }
    }

    // ---------- SiteWebViewClient.Listener ----------

    override fun onPageLoading() {
        progressBar.visibility = View.VISIBLE
    }

    override fun onPageLoaded() {
        progressBar.visibility = View.GONE
        swipeRefresh.isRefreshing = false
        hideSplashScreen()
    }

    override fun onPageFailed() {
        progressBar.visibility = View.GONE
        swipeRefresh.isRefreshing = false
        errorScreen.visibility = View.VISIBLE
    }

    override fun openExternally(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
        }
    }

    // The page's renderer stopped: start the screen again with a fresh WebView
    override fun onWebViewCrashed() = recreate()

    // ---------- SiteChromeClient.Host ----------

    override fun onProgress(percent: Int) {
        progressBar.visibility = if (percent >= 100) View.GONE else View.VISIBLE
    }

    override fun requestLocation(onResult: (Boolean) -> Unit) {
        val granted = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            onResult(true)
            return
        }
        locationResult?.invoke(false)
        locationResult = onResult
        locationPermission.launch(LOCATION_PERMISSIONS)
    }

    override fun pickFiles(pickerIntent: Intent, callback: ValueCallback<Array<Uri>>): Boolean {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        return try {
            filePicker.launch(pickerIntent)
            true
        } catch (_: ActivityNotFoundException) {
            fileCallback = null
            false
        }
    }

    private companion object {
        const val SPLASH_MILLIS = 1200L
        const val SPLASH_FADE_MILLIS = 400L
        val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
