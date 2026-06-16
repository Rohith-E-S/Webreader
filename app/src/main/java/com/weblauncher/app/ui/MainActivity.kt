package com.weblauncher.app.ui

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.weblauncher.app.ui.theme.WebLauncherTheme

/**
 * Single Activity — a thin Compose host.
 *
 * Responsibilities:
 *  - Configure edge-to-edge display and notch handling
 *  - Enter immersive sticky mode (hides status + nav bars)
 *  - Resolve the start URL (shortcut intent or homepage)
 *  - Hand off everything else to [BrowserScreen]
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: let our composables draw behind system bars
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle display cutouts (notch) — content extends into short edges
        window.attributes.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        // Enter immersive sticky mode (bars reappear transiently on swipe)
        enterImmersiveMode()

        // Determine start URL from shortcut intent or fall back to empty
        // (BrowserScreen itself reads the homepage from DataStore)
        val startUrl = intent?.data?.toString() ?: ""

        setContent {
            WebLauncherTheme {
                BrowserScreen(startUrl = startUrl)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Re-set the intent so BrowserScreen can pick up the new URL on recomposition.
        // Because we use singleTask launch mode, this is called when a shortcut is tapped
        // while the app is already running.
        setIntent(intent)
        recreate() // simplest way to re-trigger BrowserScreen with the new intent URL
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
    }

    // ── Immersive mode ────────────────────────────────────────────────────────

    private fun enterImmersiveMode() {
        val controller = window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
