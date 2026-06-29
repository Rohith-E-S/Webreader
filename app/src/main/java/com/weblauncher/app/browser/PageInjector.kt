package com.weblauncher.app.browser

/**
 * JavaScript and CSS snippets injected into pages on load.
 *
 * Injected via evaluateJavascript() from WebViewClient.onPageFinished().
 */
object PageInjector {

    /**
     * CSS to strip side margins from manhua reader pages.
     * Injected as a <style> tag so it has high specificity but can be overridden by !important rules.
     */
    private val MANHUA_CSS = """
        body, html {
            margin: 0 !important;
            padding: 0 !important;
            overflow-x: hidden !important;
        }
        .container, .site-content, .content-area, .entry-content,
        .chapter-content, .reading-content, .reader-wrapper, .reader-container,
        .chapter-view, .page-break, .read-container {
            margin: 0 auto !important;
            padding: 0 !important;
            max-width: 100vw !important;
            width: 100vw !important;
        }
        .reading-content img, .reader-area img, .page-break img, .wp-manga-chapter-img, .reader-container img, .chapter-image img, #readerarea img, .chapter-content img {
            display: block !important;
            width: 100% !important;
            max-width: 100vw !important;
            height: auto !important;
            margin: 0 auto !important;
            padding: 0 !important;
        }
    """.trimIndent().replace("\n", " ")

    /**
     * CSS classes/IDs associated with ads. Any element matching these selectors is hidden.
     */
    private val AD_REMOVAL_CSS = """
        .ad, .ads, .ad-container, .ad-wrapper, .ad-banner, .ad-block, .ad-unit,
        .banner-ad, .banner_ad, .display-ad, .advert, .advertisement, .adsbygoogle,
        .google-ad, #google_ads_div,
        iframe[src*="ad"], iframe[src*="banner"],
        .push-notification-bar, .subscribe-widget {
            display: none !important;
            visibility: hidden !important;
            pointer-events: none !important;
            height: 0 !important;
            overflow: hidden !important;
        }
    """.trimIndent().replace("\n", " ")

    /**
     * JavaScript to:
     * 1. Inject the manhua CSS and ad-removal CSS as <style> tags
     * 2. Block popups / new window opens
     * 3. Disable notification permission requests
     * 4. Remove existing ad DOM elements
     * 5. Set up a MutationObserver to remove dynamically injected ads
     */
    fun buildInjectionScript(): String = """
        (function() {
            'use strict';

            // ── 1. Inject CSS ────────────────────────────────────────────────
            function injectStyle(css) {
                var el = document.createElement('style');
                el.setAttribute('type', 'text/css');
                el.textContent = css;
                (document.head || document.documentElement).appendChild(el);
            }
            injectStyle('$MANHUA_CSS');
            injectStyle('$AD_REMOVAL_CSS');

            // ── 2. Block popup windows ───────────────────────────────────────
            window.open = function() { return null; };
            window.alert = function() {};
            window.confirm = function() { return false; };

            // ── 3. Block notification requests ───────────────────────────────
            if (window.Notification) {
                Notification.requestPermission = function() {
                    return Promise.resolve('denied');
                };
            }
            if (navigator.serviceWorker) {
                var _reg = navigator.serviceWorker.register;
                navigator.serviceWorker.register = function() {
                    return Promise.reject(new Error('SW blocked'));
                };
            }

            // ── 4. Remove existing ad DOM nodes ──────────────────────────────
            var adSelectors = [
                '.ad', '.ads', '.adsbygoogle', '.ad-container', '.ad-wrapper',
                '.banner-ad', '.popup', '.pop-up', '.popup-overlay',
                'iframe[src*="ad"]', 'iframe[src*="doubleclick"]',
                '[id*="google_ads"]', '[id*="div-gpt-ad"]'
            ];
            adSelectors.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.remove();
                });
            });

            // ── 5. MutationObserver for dynamically injected ads ─────────────
            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return; // Element nodes only
                        var cls = (node.className || '').toString();
                        var id  = (node.id || '').toString();
                        if (cls.includes('ad') || cls.includes('popup') ||
                            cls.includes('banner') || id.includes('ad') ||
                            id.includes('popup') || node.tagName === 'IFRAME') {
                            var src = node.src || '';
                            if (!src || src.includes('ad') || src.includes('doubleclick')) {
                                node.remove();
                            }
                        }
                    });
                });
            });
            observer.observe(document.documentElement, {
                childList: true,
                subtree:   true
            });

            // ── 6. Double Tap Gesture ───────────────────────────────────────
            var lastTapTime = 0;
            var startX = 0, startY = 0;
            document.addEventListener('touchstart', function(e) {
                if (e.changedTouches && e.changedTouches.length > 0) {
                    startX = e.changedTouches[0].screenX;
                    startY = e.changedTouches[0].screenY;
                }
            }, { passive: true });

            document.addEventListener('touchend', function(e) {
                if (e.changedTouches && e.changedTouches.length > 0) {
                    var endX = e.changedTouches[0].screenX;
                    var endY = e.changedTouches[0].screenY;
                    if (Math.abs(endX - startX) < 15 && Math.abs(endY - startY) < 15) {
                        var currentTime = new Date().getTime();
                        var tapLength = currentTime - lastTapTime;
                        if (tapLength < 400 && tapLength > 0) {
                            if (window.AppInterface) {
                                window.AppInterface.onDoubleTap();
                            }
                        }
                        lastTapTime = currentTime;
                    }
                }
            }, { passive: true });

        })();
    """.trimIndent()
}
