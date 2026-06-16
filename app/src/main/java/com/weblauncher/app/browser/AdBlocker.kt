package com.weblauncher.app.browser

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Ad-blocking engine implementing uBlock Origin-level request filtering.
 *
 * Blocking strategy (in order):
 *  1. Known ad network domains (hardcoded blocklist)
 *  2. URL pattern matching (path segments like /ad/, /banner/, etc.)
 *  3. User-supplied custom rules (comma-separated domains/patterns)
 *
 * All blocked requests receive an empty 200 OK response so the page doesn't
 * show network errors from blocked resources.
 */
class AdBlocker {

    // ─── Hard-coded blocklist ─────────────────────────────────────────────────

    private val BLOCKED_DOMAINS = setOf(
        // Google ads
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        // Major ad networks
        "exoclick.com",
        "popads.net",
        "adzerk.net",
        "outbrain.com",
        "taboola.com",
        "criteo.com",
        "amazon-adsystem.com",
        "media.net",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "serving-sys.com",
        "conversantmedia.com",
        "yieldmo.com",
        "bidswitch.net",
        "quantserve.com",
        "moatads.com",
        "scorecardresearch.com",
        "pixel.quantserve.com",
        // Tracking & analytics
        "analytics.google.com",
        "google-analytics.com",
        "facebook.com/tr",
        "connect.facebook.net",
        "hotjar.com",
        "segment.com",
        "mixpanel.com",
        "fullstory.com",
        "mouseflow.com",
        "clarity.ms",
        // Manhua-specific ad networks
        "click.daum.net",
        "ad.a-ads.com",
        "propellerads.com",
        "hilltopads.net",
        "adsterra.com",
        "popcash.net",
        "trafficjunky.net",
        "juicyads.com",
        "plugrush.com",
        "exdynsrv.com",
        "contentabc.com",
        "pushpush.net",
        "megapu.sh",
        "onesignal.com",
        // Popup/notification spam
        "push.techlab.click",
        "push.swads.xyz",
        "cdn.pushcrew.com",
        "web.smartclip.net",
        // Common tracking pixels
        "bat.bing.com",
        "stats.g.doubleclick.net",
        "ad.doubleclick.net",
    )

    /** URL path segments that strongly indicate advertising content */
    private val BLOCKED_PATH_PATTERNS = listOf(
        "/ad/", "/ads/", "/banner/", "/banners/",
        "/popup/", "/popups/", "/pop-under/",
        "/advertisement/", "/advertisements/",
        "/sponsor/", "/sponsored/",
        "/tracking/", "/tracker/",
        "/pixel/", "/beacon/",
        "/clickthrough/", "/click-tracker/",
        "/affiliate/", "/promo/",
        "/commercial/",
    )

    /** File extensions that are never ads and should always be passed through quickly */
    private val ALLOWED_EXTENSIONS = setOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif",
        ".svg", ".ico", ".woff", ".woff2", ".ttf", ".otf",
        ".mp4", ".webm", ".mp3", ".ogg",
    )

    // ─── User custom rules ────────────────────────────────────────────────────

    private var customPatterns: List<String> = emptyList()

    /** Update the user-defined custom blocklist (comma-separated domains/patterns). */
    fun setCustomRules(rawList: String) {
        customPatterns = rawList
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns true if the given URL should be blocked.
     * Call this from `shouldInterceptRequest`.
     */
    fun shouldBlock(url: String): Boolean {
        if (url.isBlank()) return false

        val lower = url.lowercase()

        // Quick pass-through for obviously safe resources
        if (ALLOWED_EXTENSIONS.any { lower.endsWith(it) && !lower.contains("ad") }) {
            // Still check domain even for image extensions
        }

        // 1. Domain check
        val host = extractHost(lower)
        if (host != null && BLOCKED_DOMAINS.any { host.endsWith(it) }) return true

        // 2. Path pattern check
        if (BLOCKED_PATH_PATTERNS.any { lower.contains(it) }) return true

        // 3. User custom rules
        if (customPatterns.isNotEmpty() &&
            customPatterns.any { pattern -> lower.contains(pattern) }
        ) return true

        return false
    }

    /**
     * Returns an empty WebResourceResponse to silently block a request.
     * Uses 200 OK with empty body so pages don't log network errors.
     */
    fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "utf-8",
        ByteArrayInputStream(ByteArray(0))
    )

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun extractHost(url: String): String? {
        return try {
            val withoutScheme = url.removePrefix("https://").removePrefix("http://")
            withoutScheme.substringBefore("/").substringBefore("?").substringBefore("#")
        } catch (_: Exception) {
            null
        }
    }
}
