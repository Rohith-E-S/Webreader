package com.weblauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore instance tied to application context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weblauncher_prefs")

/**
 * Repository for all persistent app preferences using Jetpack DataStore.
 * Handles homepage URL, ad blocking settings, zoom levels, and custom blocklists.
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        // General settings keys
        val KEY_HOMEPAGE_URL = stringPreferencesKey("homepage_url")
        val KEY_AD_BLOCKING_ENABLED = booleanPreferencesKey("ad_blocking_enabled")
        val KEY_CUSTOM_BLOCKLIST = stringPreferencesKey("custom_blocklist")
        val KEY_USER_AGENT_OVERRIDE = stringPreferencesKey("user_agent_override")
        val KEY_USER_AGENT_ENABLED = booleanPreferencesKey("user_agent_enabled")
        val KEY_DOMAIN_ZOOM_ENABLED = booleanPreferencesKey("domain_zoom_enabled")
        val KEY_DEFAULT_ZOOM = intPreferencesKey("default_zoom")
        val KEY_BOOKMARKS = stringSetPreferencesKey("bookmarks")
        val KEY_DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")

        // Defaults
        const val DEFAULT_HOMEPAGE = "weblauncher://bookmarks"
        const val DEFAULT_ZOOM_PERCENT = 100
        const val CHROME_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }

    // ─── Reads ───────────────────────────────────────────────────────────────

    val homepageUrl: Flow<String> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_HOMEPAGE_URL] ?: DEFAULT_HOMEPAGE }

    val adBlockingEnabled: Flow<Boolean> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_AD_BLOCKING_ENABLED] ?: true }

    val customBlocklist: Flow<String> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_CUSTOM_BLOCKLIST] ?: "" }

    val userAgentOverride: Flow<String> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_USER_AGENT_OVERRIDE] ?: CHROME_USER_AGENT }

    val userAgentEnabled: Flow<Boolean> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_USER_AGENT_ENABLED] ?: true }

    val domainZoomEnabled: Flow<Boolean> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_DOMAIN_ZOOM_ENABLED] ?: true }

    val defaultZoom: Flow<Int> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_DEFAULT_ZOOM] ?: DEFAULT_ZOOM_PERCENT }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_DARK_MODE_ENABLED] ?: false }

    val bookmarks: Flow<Set<String>> = context.dataStore.data
        .catchIOException()
        .map { prefs -> prefs[KEY_BOOKMARKS] ?: emptySet() }

    /** Reads the saved zoom percentage for a specific domain. Returns null if none saved. */
    fun getDomainZoom(domain: String): Flow<Int?> {
        val key = intPreferencesKey("zoom_$domain")
        return context.dataStore.data
            .catchIOException()
            .map { prefs -> prefs[key] }
    }

    // ─── Writes ──────────────────────────────────────────────────────────────

    suspend fun setHomepageUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[KEY_HOMEPAGE_URL] = url }
    }

    suspend fun setAdBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AD_BLOCKING_ENABLED] = enabled }
    }

    suspend fun setCustomBlocklist(list: String) {
        context.dataStore.edit { prefs -> prefs[KEY_CUSTOM_BLOCKLIST] = list }
    }

    suspend fun setUserAgentOverride(ua: String) {
        context.dataStore.edit { prefs -> prefs[KEY_USER_AGENT_OVERRIDE] = ua }
    }

    suspend fun setUserAgentEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_USER_AGENT_ENABLED] = enabled }
    }

    suspend fun setDomainZoomEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DOMAIN_ZOOM_ENABLED] = enabled }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DARK_MODE_ENABLED] = enabled }
    }

    suspend fun setDefaultZoom(percent: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_ZOOM] = percent }
    }

    /** Persists the zoom percentage for a specific domain. */
    suspend fun setDomainZoom(domain: String, percent: Int) {
        val key = intPreferencesKey("zoom_$domain")
        context.dataStore.edit { prefs -> prefs[key] = percent }
    }

    /** Clears only the zoom stored for a domain. */
    suspend fun clearDomainZoom(domain: String) {
        val key = intPreferencesKey("zoom_$domain")
        context.dataStore.edit { prefs -> prefs.remove(key) }
    }

    suspend fun addBookmark(url: String, title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS] ?: emptySet()
            prefs[KEY_BOOKMARKS] = current + "$title|$url"
        }
    }

    suspend fun removeBookmark(url: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS] ?: emptySet()
            prefs[KEY_BOOKMARKS] = current.filterNot { it.endsWith("|$url") }.toSet()
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Swallow IOException so DataStore errors don't crash the app. */
    private fun Flow<Preferences>.catchIOException(): Flow<Preferences> =
        catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
}
