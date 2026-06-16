package com.weblauncher.app.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

private const val TAG = "ManhuaWebViewClient"

/**
 * Custom WebViewClient for the Manhuwa Browser.
 *
 * Responsibilities:
 * - Request interception + ad blocking via [AdBlocker]
 * - Post-load CSS/JS injection via [PageInjector]
 * - Locked-zoom enforcement on every page load
 * - Stay-in-app URL handling (no system browser hand-off)
 * - Loading progress events for the UI progress bar
 * - Friendly error pages
 */
class ManhuaWebViewClient(
    private val context: Context,
    private val adBlocker: AdBlocker,
    private val callbacks: BrowserCallbacks,
) : WebViewClient() {

    interface BrowserCallbacks {
        fun onPageStarted(url: String?)
        fun onPageFinished(url: String?)
        fun onProgressChanged(progress: Int)
        fun isAdBlockingEnabled(): Boolean
        fun applyLockedZoom(webView: WebView)
        fun onScaleRatioChanged(ratio: Float)
    }

    // ─── Request interception ─────────────────────────────────────────────────

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url.toString()

        if (callbacks.isAdBlockingEnabled() && adBlocker.shouldBlock(url)) {
            Log.d(TAG, "BLOCKED: $url")
            return adBlocker.emptyResponse()
        }
        return null // Let WebView handle the request normally
    }

    // ─── Navigation handling ──────────────────────────────────────────────────

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val scheme = uri.scheme?.lowercase()

        // Allow http/https/about/javascript — stay inside the app
        if (scheme == "https" || scheme == "http" ||
            scheme == "about" || scheme == "javascript"
        ) {
            return false // WebView handles it
        }

        // For mailto:, tel:, intent:, market:, etc. — let the system decide
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            true // Silently ignore unresolvable intents (e.g., custom URI schemes)
        }
    }

    // ─── Lifecycle callbacks ──────────────────────────────────────────────────

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        callbacks.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)

        // Inject CSS and JS ad-removal + manhua margin fix
        val script = PageInjector.buildInjectionScript()
        view.evaluateJavascript(script, null)

        // Apply locked zoom if enabled
        callbacks.applyLockedZoom(view)

        callbacks.onPageFinished(url)
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)

        // Only show error page for the main frame (not sub-resources)
        if (request.isForMainFrame) {
            val errorHtml = buildErrorPage(
                url = request.url.toString(),
                errorCode = error.errorCode,
                description = error.description?.toString() ?: "Unknown error",
            )
            view.loadDataWithBaseURL(
                request.url.toString(),
                errorHtml,
                "text/html",
                "UTF-8",
                null,
            )
        }
    }

    // ─── Zoom tracking ────────────────────────────────────────────────────────

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        super.onScaleChanged(view, oldScale, newScale)
        if (oldScale > 0f && newScale > 0f) {
            callbacks.onScaleRatioChanged(newScale / oldScale)
        }
    }

    // ─── Error page ───────────────────────────────────────────────────────────

    private fun buildErrorPage(url: String, errorCode: Int, description: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background: #0d0d0d;
                    color: #e0e0e0;
                    font-family: -apple-system, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    padding: 24px;
                    text-align: center;
                }
                .icon { font-size: 72px; margin-bottom: 24px; }
                h1 { font-size: 22px; font-weight: 700; color: #ff6b6b; margin-bottom: 12px; }
                p  { font-size: 14px; color: #888; line-height: 1.6; margin-bottom: 8px; }
                .url { font-size: 12px; color: #555; word-break: break-all; }
                .retry-btn {
                    margin-top: 28px;
                    padding: 12px 32px;
                    background: #7b2fff;
                    color: #fff;
                    border: none;
                    border-radius: 24px;
                    font-size: 15px;
                    cursor: pointer;
                }
            </style>
        </head>
        <body>
            <div class="icon">📵</div>
            <h1>Page Failed to Load</h1>
            <p>Error $errorCode — $description</p>
            <p class="url">$url</p>
            <button class="retry-btn" onclick="location.reload()">Try Again</button>
        </body>
        </html>
    """.trimIndent()
}
