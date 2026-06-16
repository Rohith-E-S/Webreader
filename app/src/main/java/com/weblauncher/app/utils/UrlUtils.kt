package com.weblauncher.app.utils

import android.content.Context
import android.net.Uri

/**
 * Miscellaneous URL utilities used across the app.
 */
object UrlUtils {

    /**
     * Normalises user input from the address bar into a valid URL.
     * - If it looks like a URL (contains a dot and no spaces), prepend https:// if missing.
     * - Otherwise, treat as a Google search query.
     */
    fun normaliseInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return "about:blank"

        // Already has a scheme
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
            trimmed.startsWith("about:") || trimmed.startsWith("file://")
        ) return trimmed

        // Looks like a domain (e.g. "asurascans.com", "192.168.1.1")
        val looksLikeDomain = trimmed.contains(".") && !trimmed.contains(" ")
        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
    }

    /**
     * Extracts a human-readable site name from a URL for use as a shortcut label.
     * e.g. "https://asurascans.com/chapter/..." → "Asurascans"
     */
    fun siteLabel(url: String): String {
        return try {
            val host = Uri.parse(url).host ?: return "Website"
            host.removePrefix("www.")
                .split(".")
                .first()
                .replaceFirstChar { it.uppercaseChar() }
        } catch (_: Exception) {
            "Website"
        }
    }

    /**
     * Extracts the registrable domain (e.g. "asurascans.com") from a URL.
     * Used as the key for per-domain zoom storage.
     */
    fun registrableDomain(url: String): String {
        return try {
            val host = Uri.parse(url).host ?: return url
            val parts = host.removePrefix("www.").split(".")
            if (parts.size >= 2) {
                parts.takeLast(2).joinToString(".")
            } else {
                host
            }
        } catch (_: Exception) {
            url
        }
    }

    /** Returns true if the URL is a "real" web page (not about:blank or error pages). */
    fun isValidWebUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
