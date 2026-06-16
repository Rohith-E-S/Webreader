package com.weblauncher.app.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.weblauncher.app.browser.AdBlocker
import com.weblauncher.app.browser.DoubleTapWebView
import com.weblauncher.app.browser.ManhuaWebChromeClient
import com.weblauncher.app.browser.ManhuaWebViewClient
import kotlin.math.roundToInt

/**
 * Thin Compose wrapper around Android's [WebView].
 *
 * Uses [AndroidView] to embed the WebView inside the Compose tree.
 * The [WebView] is created once and reused — avoiding expensive re-creation
 * on recomposition. All browser logic stays in [ManhuaWebViewClient] and
 * [ManhuaWebChromeClient].
 *
 * Double-tap is detected via a native [GestureDetector] so it works even
 * while the page is still loading (JS isn't injected yet in that state).
 *
 * @param onDoubleTap  Callback triggered when a double tap is detected.
 * @param webViewRef   Callback to expose the WebView instance to the parent so it
 *                     can call loadUrl, goBack, reload, etc.
 */
@Composable
fun BrowserWebView(
    adBlocker: AdBlocker,
    adBlockingEnabled: Boolean,
    zoomLocked: Boolean,
    zoomPercent: Int,
    userAgent: String?,
    darkModeEnabled: Boolean,
    isRefreshing: Boolean,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onFaviconReceived: (android.graphics.Bitmap?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onDoubleTap: () -> Unit,
    onZoomChange: (Int, Boolean) -> Unit,
    onUserInteraction: () -> Unit,
    onRefresh: () -> Unit,
    webViewRef: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            // ── SwipeRefreshLayout wrapping the WebView ──────────────────────
            val swipeLayout = SwipeRefreshLayout(context).apply {
                setColorSchemeColors(
                    "#7b2fff".toColorInt(),
                    "#00c6ff".toColorInt(),
                )
                setProgressBackgroundColorSchemeColor(
                    "#1a1a2e".toColorInt()
                )
            }

            val wv = DoubleTapWebView(context)
            configureWebView(
                wv                  = wv,
                adBlocker           = adBlocker,
                adBlockingEnabled   = adBlockingEnabled,
                zoomLocked          = zoomLocked,
                zoomPercent         = zoomPercent,
                userAgent           = userAgent,
                darkModeEnabled     = darkModeEnabled,
                onPageStarted       = onPageStarted,
                onPageFinished      = onPageFinished,
                onTitleChanged      = onTitleChanged,
                onFaviconReceived   = onFaviconReceived,
                onProgressChanged   = onProgressChanged,
                onZoomChange        = onZoomChange,
            )

            // Touch listener only for focus-clear signal.
            // Double-tap is handled entirely inside DoubleTapWebView.dispatchTouchEvent.
            @SuppressLint("ClickableViewAccessibility")
            wv.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) onUserInteraction()
                false
            }

            // Set the initial double-tap callback.
            wv.onDoubleTapCallback = onDoubleTap

            // Disable SwipeRefresh when WebView can scroll up (avoids conflicts)
            swipeLayout.setOnChildScrollUpCallback { _, _ ->
                wv.scrollY > 0
            }
            swipeLayout.setOnRefreshListener {
                wv.reload()
                onRefresh()
            }

            swipeLayout.addView(
                wv,
                android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                )
            )

            webViewRef(wv)
            swipeLayout
        },
        update = { swipeLayout ->
            val wv = swipeLayout.getChildAt(0) as? DoubleTapWebView ?: return@AndroidView
            // Keep the callback current on every recomposition (prevents stale lambda).
            wv.onDoubleTapCallback = onDoubleTap
            // Guard all settings writes so we only touch WebView when values actually change.
            // Unconditional writes trigger internal re-renders causing grey flashes.
            val newUa = userAgent
            if (wv.settings.userAgentString != newUa) wv.settings.userAgentString = newUa
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (wv.settings.isAlgorithmicDarkeningAllowed != darkModeEnabled)
                    wv.settings.isAlgorithmicDarkeningAllowed = darkModeEnabled
            }
            if (zoomLocked) wv.setInitialScale(zoomPercent)
            // Properly sync the pull-to-refresh spinner state.
            if (swipeLayout.isRefreshing != isRefreshing) swipeLayout.isRefreshing = isRefreshing
        },
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// WebView configuration helper
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(
    wv: WebView,
    adBlocker: AdBlocker,
    adBlockingEnabled: Boolean,
    zoomLocked: Boolean,
    zoomPercent: Int,
    userAgent: String?,
    darkModeEnabled: Boolean,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onFaviconReceived: (android.graphics.Bitmap?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onZoomChange: (Int, Boolean) -> Unit,
) {
    with(wv.settings) {
        javaScriptEnabled              = true
        domStorageEnabled              = true
        databaseEnabled                = true
        loadsImagesAutomatically       = true
        mediaPlaybackRequiresUserGesture = false

        // Zoom
        setSupportZoom(true)
        builtInZoomControls   = true
        displayZoomControls   = false   // hide ugly +/- overlay buttons
        useWideViewPort       = true
        loadWithOverviewMode  = false

        // Cache
        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        // Allow mixed content for some CDNs
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // User agent
        if (userAgent != null) userAgentString = userAgent

        // Dark mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            isAlgorithmicDarkeningAllowed = darkModeEnabled
        }
    }

    // Initial zoom if locked
    if (zoomLocked) wv.setInitialScale(zoomPercent)

    // Suppress long-press context menus
    wv.isLongClickable = false
    wv.setOnLongClickListener { true }

    // Match WebView background to app's dark background.
    // This eliminates the white/grey flash when the WebView surface is redrawn
    // (e.g., on page load completion) — instead of grey, users see dark background.
    wv.setBackgroundColor(android.graphics.Color.parseColor("#0d0d0d"))

    // Scroll performance
    wv.overScrollMode = android.view.View.OVER_SCROLL_NEVER

    // Provide mutable flags via wrapper objects so callbacks can read latest values
    val adEnabledHolder  = BoolHolder(adBlockingEnabled)
    val zoomLockedHolder = BoolHolder(zoomLocked)
    val zoomPctHolder    = IntHolder(zoomPercent)

    wv.webViewClient = ManhuaWebViewClient(
        context   = wv.context,
        adBlocker = adBlocker,
        callbacks = object : ManhuaWebViewClient.BrowserCallbacks {
            override fun onPageStarted(url: String?)         { onPageStarted(url) }
            override fun onPageFinished(url: String?)        { onPageFinished(url) }
            override fun onProgressChanged(progress: Int)    { onProgressChanged(progress) }
            override fun isAdBlockingEnabled()               = adEnabledHolder.value
            override fun applyLockedZoom(webView: WebView) {
                if (zoomLockedHolder.value) webView.setInitialScale(zoomPctHolder.value)
            }
            override fun onScaleRatioChanged(ratio: Float) {
                val current = zoomPctHolder.value
                val updated = (current * ratio).roundToInt().coerceIn(50, 200)
                if (updated != current) {
                    zoomPctHolder.value = updated
                    onZoomChange(updated, true)
                }
            }
        },
    )

    wv.webChromeClient = ManhuaWebChromeClient(
        onProgressChanged = onProgressChanged,
        onTitleChanged    = onTitleChanged,
        onFaviconReceived = onFaviconReceived,
    )
}

/** Mutable boolean holder used to bridge Compose state into WebViewClient callbacks. */
private class BoolHolder(var value: Boolean)
private class IntHolder(var value: Int)
