package com.weblauncher.app.browser

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView

/**
 * Custom WebView that intercepts touch events at the [dispatchTouchEvent] level.
 *
 * Key design decisions:
 * 1. [dispatchTouchEvent] fires BEFORE WebView's internal gesture handling, giving
 *    us first access to the raw touch stream — works during loading too.
 * 2. When a double-tap is detected, we CONSUME the event (return true without
 *    calling super) so WebView's built-in double-tap-to-zoom never fires.
 * 3. [onDoubleTapCallback] is a var so the parent can update it after construction
 *    (fixes stale-lambda issue with AndroidView's one-shot factory).
 */
@SuppressLint("ViewConstructor")
class DoubleTapWebView(context: Context) : WebView(context) {

    /** Updated by the parent after every recomposition to avoid stale captures. */
    var onDoubleTapCallback: (() -> Unit)? = null

    /** Set to true by the GestureDetector callback before dispatchTouchEvent returns. */
    private var pendingDoubleTap = false

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            // Must return true so GestureDetector tracks the full event sequence.
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                pendingDoubleTap = true
                return true
            }
        }
    ).also {
        it.setIsLongpressEnabled(false)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        pendingDoubleTap = false

        // Feed to our detector FIRST, before WebView sees anything.
        gestureDetector.onTouchEvent(ev)

        return if (pendingDoubleTap) {
            // Double-tap confirmed: fire callback and CONSUME the event so
            // WebView's own double-tap-to-zoom handler never runs.
            post { onDoubleTapCallback?.invoke() }
            true
        } else {
            // Normal touch: let WebView handle it as usual.
            super.dispatchTouchEvent(ev)
        }
    }
}
