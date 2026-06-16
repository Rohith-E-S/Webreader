package com.weblauncher.app.browser

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ManhuaChrome"

/**
 * Custom WebChromeClient providing:
 * - Loading progress callbacks
 * - Page title callbacks
 * - Favicon capture for home screen shortcuts
 * - Popup window blocking (onCreateWindow returns false)
 *
 * All UI updates are forwarded via lambdas so this class has no direct
 * dependency on View or Compose state.
 */
class ManhuaWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onTitleChanged: (String) -> Unit,
    private val onFaviconReceived: (Bitmap?) -> Unit,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleChanged(it) }
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        onFaviconReceived(icon)
    }

    /** Block all popup windows. */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?,
    ): Boolean = false
}

// ─────────────────────────────────────────────────────────────────────────────
// Home-screen shortcut creator
// ─────────────────────────────────────────────────────────────────────────────

object ShortcutCreator {

    fun createShortcut(
        activity: Activity,
        scope: CoroutineScope,
        url: String,
        title: String,
        favicon: Bitmap?,
    ) {
        scope.launch(Dispatchers.IO) {
            val icon = favicon
                ?: downloadFavicon(url)
                ?: generateFallbackIcon(title)

            withContext(Dispatchers.Main) {
                pinShortcut(activity, url, title, icon)
            }
        }
    }

    private fun pinShortcut(activity: Activity, url: String, label: String, icon: Bitmap) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {
            Log.w(TAG, "Pinned shortcuts not supported on this launcher")
            return
        }

        val iconCompat = IconCompat.createWithAdaptiveBitmap(icon)
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            Uri.parse(url),
            activity,
            activity::class.java,
        )

        val shortcutId = "shortcut_${url.hashCode()}"
        val info = ShortcutInfoCompat.Builder(activity, shortcutId)
            .setShortLabel(label.take(10))
            .setLongLabel(label.take(25))
            .setIcon(iconCompat)
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(activity, info, null)
        Log.d(TAG, "Pinned shortcut: $shortcutId → $url")
    }

    private fun downloadFavicon(pageUrl: String): Bitmap? {
        return try {
            val host = Uri.parse(pageUrl).host ?: return null
            val faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=$host"
            val conn = URL(faviconUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout    = 4_000
            conn.connect()
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                android.graphics.BitmapFactory.decodeStream(conn.inputStream)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Favicon download failed: ${e.message}")
            null
        }
    }

    private fun generateFallbackIcon(label: String): Bitmap {
        val size   = 192
        val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#7B2FFF"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = Color.WHITE
            textSize  = 72f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val initial = label.take(2).uppercase()
        val y = (size / 2f) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(initial, size / 2f, y, paint)
        return bmp
    }

    @Suppress("unused")
    private fun saveIconToFile(context: Context, bitmap: Bitmap, label: String): File? {
        return try {
            val dir  = File(context.cacheDir, "favicons").apply { mkdirs() }
            val file = File(dir, "${label.hashCode()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (_: Exception) { null }
    }
}
