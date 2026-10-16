package com.pantech.ismltsapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var splashScreen: LinearLayout
    private lateinit var errorScreen: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var headerTitle: TextView
    private lateinit var profileButton: ImageView
    private lateinit var homeGridContainer: View
    private lateinit var webViewContainer: View
    private lateinit var tileAnnouncements: LinearLayout
    private lateinit var tileProgress: LinearLayout
    private lateinit var tileCalendar: LinearLayout
    private lateinit var tileProfile: LinearLayout

    private val homeUrl       = "https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net/"
    private val messagesUrl   = "https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net/Announcements"
    private val favouritesUrl = "https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net/Insights/MyProgress"
    private val menuUrl       = "https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net/Calendar"
    private val profileUrl    = "https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net/Profile"

    private var currentNavItem = R.id.nav_home

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        webView           = findViewById(R.id.webView)
        progressBar       = findViewById(R.id.progressBar)
        splashScreen      = findViewById(R.id.splashScreen)
        errorScreen       = findViewById(R.id.errorScreen)
        swipeRefresh      = findViewById(R.id.swipeRefresh)
        bottomNav         = findViewById(R.id.bottomNav)
        headerTitle       = findViewById(R.id.headerTitle)
        profileButton     = findViewById(R.id.profileButton)
        homeGridContainer = findViewById(R.id.homeGridContainer)
        webViewContainer  = findViewById(R.id.webViewContainer)
        tileAnnouncements = findViewById(R.id.tileAnnouncements)
        tileProgress      = findViewById(R.id.tileProgress)
        tileCalendar      = findViewById(R.id.tileCalendar)
        tileProfile       = findViewById(R.id.tileProfile)

        setupWebView()
        setupRefresh()
        setupRetryButton()
        setupBackButton()
        setupHeader()
        setupBottomNavigation()
        setupHomeGrid()

        showHomeGrid()
        Handler(Looper.getMainLooper()).postDelayed({ hideSplashScreen() }, 1200)
    }

    private fun setupHomeGrid() {
        tileAnnouncements.setOnClickListener {
            showWebViewPage(messagesUrl, getString(R.string.nav_messages), R.id.nav_messages)
        }
        tileProgress.setOnClickListener {
            showWebViewPage(favouritesUrl, getString(R.string.nav_favourites), R.id.nav_favourites)
        }
        tileCalendar.setOnClickListener {
            showWebViewPage(menuUrl, getString(R.string.nav_menu), R.id.nav_menu)
        }
        tileProfile.setOnClickListener {
            showWebViewPage(profileUrl, getString(R.string.nav_profile), null)
        }
    }

    private fun showHomeGrid() {
        homeGridContainer.visibility = View.VISIBLE
        webViewContainer.visibility  = View.GONE
        errorScreen.visibility       = View.GONE
        progressBar.visibility       = View.GONE
        headerTitle.text = getString(R.string.nav_home)
        currentNavItem = R.id.nav_home
        setBottomNavSelection(R.id.nav_home)
    }

    private fun showWebViewPage(url: String, title: String, navItemId: Int?) {
        homeGridContainer.visibility = View.GONE
        webViewContainer.visibility  = View.VISIBLE
        if (navItemId != null) currentNavItem = navItemId
        setBottomNavSelection(navItemId)
        loadPage(url, title)
    }

    private fun setBottomNavSelection(itemId: Int?) {
        for (i in 0 until bottomNav.menu.size()) {
            val item = bottomNav.menu.getItem(i)
            item.isChecked = (item.itemId == itemId)
        }
    }

    private fun loadPage(url: String, title: String) {
        errorScreen.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        headerTitle.text = title
        if (webView.url != url) webView.loadUrl(url)
    }

    private fun setupHeader() {
        profileButton.setOnClickListener {
            showWebViewPage(profileUrl, getString(R.string.nav_profile), null)
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showHomeGrid(); true }
                R.id.nav_messages -> {
                    showWebViewPage(messagesUrl, getString(R.string.nav_messages), R.id.nav_messages); true
                }
                R.id.nav_favourites -> {
                    showWebViewPage(favouritesUrl, getString(R.string.nav_favourites), R.id.nav_favourites); true
                }
                R.id.nav_menu -> {
                    showWebViewPage(menuUrl, getString(R.string.nav_menu), R.id.nav_menu); true
                }
                else -> false
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled               = true
        settings.domStorageEnabled               = true
        settings.databaseEnabled                 = true
        settings.useWideViewPort                 = true
        settings.loadWithOverviewMode            = true
        settings.cacheMode                       = WebSettings.LOAD_DEFAULT
        settings.builtInZoomControls             = false
        settings.displayZoomControls             = false
        settings.textZoom                        = 100
        settings.setSupportMultipleWindows(false)
        settings.mediaPlaybackRequiresUserGesture = false

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.setBackgroundColor(getColor(R.color.white))

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                if (!url.isNullOrEmpty()) view?.loadUrl(url)
                return true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility    = View.GONE
                swipeRefresh.isRefreshing = false
                hideSplashScreen()
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    progressBar.visibility    = View.GONE
                    swipeRefresh.isRefreshing = false
                    showErrorScreen()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }
    }

    private fun hideSplashScreen() {
        if (splashScreen.visibility != View.GONE) {
            splashScreen.animate().alpha(0f).setDuration(400)
                .withEndAction { splashScreen.visibility = View.GONE }.start()
        }
    }

    private fun showErrorScreen() { errorScreen.visibility = View.VISIBLE }

    private fun setupRefresh() {
        swipeRefresh.setOnRefreshListener { webView.reload() }
        swipeRefresh.setColorSchemeResources(R.color.primary_blue)
    }

    private fun setupRetryButton() {
        findViewById<Button>(R.id.retryButton).setOnClickListener {
            errorScreen.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            webView.reload()
        }
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webViewContainer.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
                    webViewContainer.visibility == View.VISIBLE -> showHomeGrid()
                    else -> finish()
                }
            }
        })
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.clearHistory()
        webView.destroy()
        super.onDestroy()
    }
}