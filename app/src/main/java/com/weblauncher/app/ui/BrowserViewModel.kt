package com.weblauncher.app.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weblauncher.app.data.PreferencesRepository
import com.weblauncher.app.ui.components.AppSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for [BrowserScreen] / [MainActivity].
 *
 * Holds and exposes:
 * - All user preferences (StateFlows backed by DataStore)
 * - Current URL and page title
 * - Zoom state (locked flag + current percentage)
 * - Favicon bitmap (for shortcut creation)
 *
 * Survives configuration changes (rotations, keyboard visibility).
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    // ─── Preferences (StateFlow from DataStore) ───────────────────────────────

    val homepageUrl: StateFlow<String> = prefs.homepageUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesRepository.DEFAULT_HOMEPAGE)

    val adBlockingEnabled: StateFlow<Boolean> = prefs.adBlockingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val customBlocklist: StateFlow<String> = prefs.customBlocklist
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val userAgentEnabled: StateFlow<Boolean> = prefs.userAgentEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val userAgentOverride: StateFlow<String> = prefs.userAgentOverride
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesRepository.CHROME_USER_AGENT)

    val domainZoomEnabled: StateFlow<Boolean> = prefs.domainZoomEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val defaultZoom: StateFlow<Int> = prefs.defaultZoom
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesRepository.DEFAULT_ZOOM_PERCENT)

    val darkModeEnabled: StateFlow<Boolean> = prefs.darkModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val bookmarks: StateFlow<Set<String>> = prefs.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // ─── Zoom state ───────────────────────────────────────────────────────────

    private val _zoomPercent = MutableStateFlow(PreferencesRepository.DEFAULT_ZOOM_PERCENT)
    val zoomPercent: StateFlow<Int> = _zoomPercent.asStateFlow()

    private val _zoomLocked = MutableStateFlow(false)
    val zoomLocked: StateFlow<Boolean> = _zoomLocked.asStateFlow()

    init {
        // Sync in-memory zoom with stored default on first load
        viewModelScope.launch {
            prefs.defaultZoom.first().let { _zoomPercent.value = it }
        }
    }

    fun setZoomPercent(percent: Int) {
        _zoomPercent.value = percent.coerceIn(50, 200)
    }

    fun toggleZoomLock() {
        _zoomLocked.value = !_zoomLocked.value
    }

    // ─── Current URL / title ──────────────────────────────────────────────────

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    fun setCurrentUrl(url: String?)   { _currentUrl.value   = url }
    fun setCurrentTitle(title: String?) { _currentTitle.value = title }

    // ─── Favicon ──────────────────────────────────────────────────────────────

    private val _favicon = MutableStateFlow<Bitmap?>(null)
    val favicon: StateFlow<Bitmap?> = _favicon.asStateFlow()

    fun setFavicon(bmp: Bitmap?) { _favicon.value = bmp }

    // ─── Bookmarks ────────────────────────────────────────────────────────────

    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            val current = bookmarks.value
            val existing = current.find { it.endsWith("|$url") }
            if (existing != null) {
                prefs.removeBookmark(url)
            } else {
                prefs.addBookmark(url, title)
            }
        }
    }

    // ─── Per-domain zoom ──────────────────────────────────────────────────────

    fun getDomainZoomFlow(domain: String) = prefs.getDomainZoom(domain)

    fun saveDomainZoom(domain: String, percent: Int) {
        viewModelScope.launch { prefs.setDomainZoom(domain, percent) }
    }

    // ─── Settings persistence ─────────────────────────────────────────────────

    fun applySettings(settings: AppSettings) {
        viewModelScope.launch {
            prefs.setHomepageUrl(settings.homepageUrl)
            prefs.setAdBlockingEnabled(settings.adBlockingEnabled)
            prefs.setCustomBlocklist(settings.customBlocklist)
            prefs.setUserAgentEnabled(settings.userAgentEnabled)
            prefs.setUserAgentOverride(settings.userAgentOverride)
            prefs.setDomainZoomEnabled(settings.domainZoomEnabled)
            prefs.setDefaultZoom(settings.defaultZoom)
            prefs.setDarkModeEnabled(settings.darkModeEnabled)
        }
        setZoomPercent(settings.defaultZoom)
    }
}
