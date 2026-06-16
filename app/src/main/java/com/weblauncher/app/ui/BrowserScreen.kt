package com.weblauncher.app.ui

import android.app.Activity
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.weblauncher.app.browser.AdBlocker
import com.weblauncher.app.browser.ShortcutCreator
import com.weblauncher.app.ui.components.AppSettings
import com.weblauncher.app.ui.components.BrowserToolbar
import com.weblauncher.app.ui.components.BrowserWebView
import com.weblauncher.app.ui.components.SettingsSheet
import com.weblauncher.app.ui.theme.*
import com.weblauncher.app.utils.UrlUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch


/**
 * Root Compose screen that assembles the entire browser UI:
 *  - Full-screen WebView
 *  - Floating toolbar (slide in/out)
 *  - Thin loading progress bar
 *  - Settings sheet (dialog overlay)
 *  - Gesture detection for top-edge tap to reveal toolbar
 */
@Composable
fun BrowserScreen(
    startUrl: String,
    viewModel: BrowserViewModel = viewModel(),
) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val scope    = rememberCoroutineScope()

    // ── State from ViewModel (DataStore-backed) ───────────────────────────────
    val adBlockEnabled  by viewModel.adBlockingEnabled.collectAsStateWithLifecycle()
    val customBlocklist by viewModel.customBlocklist.collectAsStateWithLifecycle()
    val uaEnabled       by viewModel.userAgentEnabled.collectAsStateWithLifecycle()
    val uaString        by viewModel.userAgentOverride.collectAsStateWithLifecycle()
    val domainZoomOn    by viewModel.domainZoomEnabled.collectAsStateWithLifecycle()
    val homepageUrl     by viewModel.homepageUrl.collectAsStateWithLifecycle()
    val zoomPercent     by viewModel.zoomPercent.collectAsStateWithLifecycle()
    val zoomLocked      by viewModel.zoomLocked.collectAsStateWithLifecycle()
    val currentUrl      by viewModel.currentUrl.collectAsStateWithLifecycle()
    val favicon         by viewModel.favicon.collectAsStateWithLifecycle()
    val bookmarks       by viewModel.bookmarks.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()

    // ── Local UI state ────────────────────────────────────────────────────────
    var toolbarVisible    by remember { mutableStateOf(true) }
    var showSettings      by remember { mutableStateOf(false) }
    var loadingProgress   by remember { mutableIntStateOf(0) }
    var isLoading         by remember { mutableStateOf(false) }
    var canGoBack         by remember { mutableStateOf(false) }
    var canGoForward      by remember { mutableStateOf(false) }
    var pageTitle         by remember { mutableStateOf("") }
    var lastFavicon       by remember { mutableStateOf<Bitmap?>(null) }
    var lastAppliedZoom   by remember { mutableIntStateOf(zoomPercent) }
    var isPullRefreshing  by remember { mutableStateOf(false) }

    // Focus manager for clearing text field focus
    val focusManager = LocalFocusManager.current

    // WebView reference (bridge from AndroidView to Compose actions)
    var webView by remember { mutableStateOf<WebView?>(null) }

    // AdBlocker singleton
    val adBlocker = remember { AdBlocker() }
    LaunchedEffect(customBlocklist) { adBlocker.setCustomRules(customBlocklist) }

    // ── Toolbar auto-hide logic ───────────────────────────────────────────────
    // Removed timeout, it only toggles on double tap now.


    // Load start URL (and apply saved domain zoom)
    LaunchedEffect(startUrl) {
        if (domainZoomOn) {
            val domain = UrlUtils.registrableDomain(startUrl)
            viewModel.getDomainZoomFlow(domain).collect { saved ->
                if (saved != null) viewModel.setZoomPercent(saved)
            }
        }
        webView?.loadUrl(startUrl)
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    // ── Root box: WebView behind toolbar ─────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── WebView ──────────────────────────────────────────────────────────
        BrowserWebView(
            adBlocker         = adBlocker,
            adBlockingEnabled = adBlockEnabled,
            zoomLocked        = zoomLocked,
            zoomPercent       = zoomPercent,
            userAgent         = if (uaEnabled) uaString else null,
            darkModeEnabled   = darkModeEnabled,
            isRefreshing      = isPullRefreshing,
            onRefresh         = { isPullRefreshing = true },
            onPageStarted     = { url ->
                isLoading    = true
                loadingProgress = 0
                canGoBack    = webView?.canGoBack()    ?: false
                canGoForward = webView?.canGoForward() ?: false
                viewModel.setCurrentUrl(url)
            },
            onPageFinished = { url ->
                isLoading = false
                loadingProgress = 100
                isPullRefreshing = false
                canGoBack    = webView?.canGoBack()    ?: false
                canGoForward = webView?.canGoForward() ?: false
                viewModel.setCurrentUrl(url)
                // Persist domain zoom when locked
                if (zoomLocked && domainZoomOn && url != null) {
                    val domain = UrlUtils.registrableDomain(url)
                    viewModel.saveDomainZoom(domain, zoomPercent)
                }
                lastAppliedZoom = zoomPercent
            },
            onTitleChanged    = { title -> pageTitle = title },
            onFaviconReceived = { bmp ->
                lastFavicon = bmp
                viewModel.setFavicon(bmp)
            },
            onProgressChanged = { progress ->
                loadingProgress = progress
                isLoading = progress < 100
            },
            onDoubleTap = {
                toolbarVisible = !toolbarVisible
            },
            onZoomChange = { percent, fromPinch ->
                viewModel.setZoomPercent(percent)
                if (!fromPinch) {
                    val factor = percent.toFloat() / lastAppliedZoom.toFloat()
                    webView?.zoomBy(factor)
                }
                lastAppliedZoom = percent
            },
            onUserInteraction = {
                focusManager.clearFocus()
            },
            webViewRef = { wv -> webView = wv },
            modifier   = Modifier.fillMaxSize(),
        )

        // ── Bookmarks Overlay ────────────────────────────────────────────────
        if (currentUrl == "weblauncher://bookmarks") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { focusManager.clearFocus() }
                        )
                    }
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "Bookmarks",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp, top = 140.dp)
                    )
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet.", color = TextSecondary)
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(bookmarks.toList()) { b ->
                                val parts = b.split("|", limit = 2)
                                val bTitle = parts.getOrNull(0) ?: "Unknown"
                                val bUrl = parts.getOrNull(1) ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            webView?.loadUrl(bUrl)
                                            toolbarVisible = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val domain = android.net.Uri.parse(bUrl).host ?: bUrl
                                    AsyncImage(
                                        model = "https://www.google.com/s2/favicons?domain=$domain&sz=64",
                                        contentDescription = "Favicon",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(bTitle, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(bUrl, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { viewModel.toggleBookmark(bUrl, bTitle) }) {
                                        Icon(Icons.Default.Delete, "Remove bookmark", tint = AccentRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Loading progress bar (thin, top of screen) ───────────────────────
        AnimatedVisibility(
            visible = isLoading,
            enter   = fadeIn(tween(150)),
            exit    = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            LinearProgressIndicator(
                progress      = { loadingProgress / 100f },
                color         = Purple500,
                trackColor    = Purple900,
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }

        // ── Floating toolbar ─────────────────────────────────────────────────
        BrowserToolbar(
            visible        = toolbarVisible,
            currentUrl     = currentUrl ?: "",
            canGoBack      = canGoBack,
            canGoForward   = canGoForward,
            isLoading      = isLoading,
            zoomPercent    = zoomPercent,
            zoomLocked     = zoomLocked,
            adBlockEnabled = adBlockEnabled,
            isBookmarked   = bookmarks.any { it.endsWith("|${currentUrl ?: ""}") },
            onBack = {
                webView?.goBack()
            },
            onForward = {
                webView?.goForward()
            },
            onRefresh = {
                if (isLoading) webView?.stopLoading() else webView?.reload()
            },
            onHome = {
                webView?.loadUrl(homepageUrl)
            },
            onNavigate = { input ->
                val url = UrlUtils.normaliseInput(input)
                webView?.loadUrl(url)
                toolbarVisible = false
            },
            onZoomChange = { percent, fromPinch ->
                viewModel.setZoomPercent(percent)
                if (!fromPinch) {
                    val factor = percent.toFloat() / lastAppliedZoom.toFloat()
                    webView?.zoomBy(factor)
                }
                lastAppliedZoom = percent
            },
            onZoomLockToggle = {
                viewModel.toggleZoomLock()
                val nowLocked = !zoomLocked
                if (nowLocked) {
                    webView?.setInitialScale(zoomPercent)
                } else {
                    webView?.setInitialScale(0)
                }
            },
            onBookmarkToggle = {
                currentUrl?.let { url ->
                    viewModel.toggleBookmark(url, pageTitle.takeIf { it.isNotBlank() } ?: url)
                }
            },
            onSaveAsApp = {
                val url = webView?.url ?: return@BrowserToolbar
                val label = UrlUtils.siteLabel(url)
                activity?.let {
                    ShortcutCreator.createShortcut(
                        activity = it,
                        scope    = scope,
                        url      = url,
                        title    = label,
                        favicon  = lastFavicon,
                    )
                }
            },
            onSettings = {
                showSettings = true
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // ── Settings dialog ──────────────────────────────────────────────────
        if (showSettings) {
            SettingsSheet(
                initialSettings = AppSettings(
                    homepageUrl       = homepageUrl,
                    adBlockingEnabled = adBlockEnabled,
                    customBlocklist   = customBlocklist,
                    userAgentEnabled  = uaEnabled,
                    userAgentOverride = uaString,
                    domainZoomEnabled = domainZoomOn,
                    defaultZoom       = zoomPercent,
                    darkModeEnabled   = darkModeEnabled,
                ),
                onSave = { settings ->
                    viewModel.applySettings(settings)
                    showSettings = false
                },
                onClearCache = {
                    webView?.clearCache(true)
                    webView?.clearHistory()
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                },
                onDismiss = { showSettings = false },
            )
        }
    }
}
