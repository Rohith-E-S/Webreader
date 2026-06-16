package com.weblauncher.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weblauncher.app.ui.theme.*

/**
 * Floating browser toolbar rendered at the top of the screen.
 *
 * Two rows:
 *  Row 1 — navigation controls (back/forward/refresh/home) + address bar + action icons
 *  Row 2 — zoom slider with percentage label and lock toggle
 *
 * Animated with slide-in/out on [visible] change.
 */
@Composable
fun BrowserToolbar(
    visible: Boolean,
    currentUrl: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    zoomPercent: Int,
    zoomLocked: Boolean,
    adBlockEnabled: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onNavigate: (String) -> Unit,
    onZoomChange: (Int, Boolean) -> Unit,
    onZoomLockToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onSaveAsApp: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(
            initialOffsetY = { -it },
            animationSpec  = tween(durationMillis = 260),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 220),
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ToolbarBg)
                .statusBarsPadding()          // respect notch/status-bar insets
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            // ── Row 1: Nav + Address bar + Actions ───────────────────────────
            NavigationRow(
                currentUrl    = currentUrl,
                canGoBack     = canGoBack,
                canGoForward  = canGoForward,
                isLoading     = isLoading,
                adBlockEnabled = adBlockEnabled,
                isBookmarked   = isBookmarked,
                onBack        = onBack,
                onForward     = onForward,
                onRefresh     = onRefresh,
                onHome        = onHome,
                onNavigate    = onNavigate,
                onBookmarkToggle = onBookmarkToggle,
                onSaveAsApp   = onSaveAsApp,
                onSettings    = onSettings,
            )

            Spacer(Modifier.height(2.dp))

            // ── Row 2: Zoom slider ────────────────────────────────────────────
            ZoomRow(
                zoomPercent   = zoomPercent,
                zoomLocked    = zoomLocked,
                onZoomChange  = onZoomChange,
                onLockToggle  = onZoomLockToggle,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NavigationRow(
    currentUrl: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    adBlockEnabled: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onNavigate: (String) -> Unit,
    onBookmarkToggle: () -> Unit,
    onSaveAsApp: () -> Unit,
    onSettings: () -> Unit,
) {
    var addressInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        if (!isFocused) {
            // Back
        ToolbarIconButton(
            icon        = Icons.AutoMirrored.Filled.ArrowBack,
            contentDesc = "Back",
            enabled     = canGoBack,
            onClick     = onBack,
        )
        // Forward
        ToolbarIconButton(
            icon        = Icons.AutoMirrored.Filled.ArrowForward,
            contentDesc = "Forward",
            enabled     = canGoForward,
            onClick     = onForward,
        )
        // Refresh / Stop
        ToolbarIconButton(
            icon        = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
            contentDesc = if (isLoading) "Stop" else "Refresh",
            onClick     = onRefresh,
        )
        // Home
        ToolbarIconButton(
            icon        = Icons.Default.Home,
            contentDesc = "Home",
            onClick     = onHome,
        )

        }

        // Address bar
        AddressBar(
            value      = addressInput,
            onValueChange = { addressInput = it },
            onGo       = { onNavigate(addressInput) },
            isFocused  = isFocused,
            onFocusChange = { isFocused = it },
            modifier   = Modifier.weight(1f),
        )

        if (!isFocused) {
            // Ad-block indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(if (adBlockEnabled) AccentGreen else AccentRed)
                .padding(start = 4.dp),
        )
        Spacer(Modifier.width(4.dp))

        // Bookmark
        ToolbarIconButton(
            icon        = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkAdd,
            contentDesc = "Bookmark",
            tint        = if (isBookmarked) Purple500 else TextPrimary,
            onClick     = onBookmarkToggle,
        )

        // Save as App
        ToolbarIconButton(
            icon        = Icons.AutoMirrored.Filled.AddToHomeScreen,
            contentDesc = "Save as App",
            onClick     = onSaveAsApp,
        )
        // Settings
        ToolbarIconButton(
            icon        = Icons.Default.Settings,
            contentDesc = "Settings",
            onClick     = onSettings,
        )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Address bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddressBar(
    value: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontSize by animateFloatAsState(targetValue = if (isFocused) 16f else 12f)
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, color = TextPrimary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction    = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(onGo = { 
            onGo() 
            focusManager.clearFocus()
        }),
        modifier = modifier
            .height(38.dp)
            .background(SurfaceVariant, RoundedCornerShape(20.dp))
            .onFocusChanged { onFocusChange(it.isFocused) }
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text("Search or enter URL", fontSize = fontSize.sp,
                        color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                innerTextField()
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Zoom row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZoomRow(
    zoomPercent: Int,
    zoomLocked: Boolean,
    onZoomChange: (Int, Boolean) -> Unit,
    onLockToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            imageVector        = Icons.Default.ZoomIn,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))

        Text(
            text     = "$zoomPercent%",
            color    = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.width(38.dp),
        )

        Slider(
            value         = zoomPercent.toFloat(),
            onValueChange = { onZoomChange(it.toInt(), false) },
            valueRange    = 50f..200f,
            steps         = 29,        // 5% increments from 50 to 200 = 30 steps - 1
            colors        = SliderDefaults.colors(
                thumbColor        = if (zoomLocked) AccentAmber else Purple500,
                activeTrackColor  = if (zoomLocked) AccentAmber else Purple500,
                inactiveTrackColor = SurfaceElevated,
            ),
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(4.dp))

        // Lock toggle
        IconButton(
            onClick  = onLockToggle,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector        = if (zoomLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (zoomLocked) "Zoom locked" else "Zoom unlocked",
                tint               = if (zoomLocked) AccentAmber else TextSecondary,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable icon button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = TextPrimary,
) {
    IconButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDesc,
            tint               = if (enabled) tint else TextSecondary.copy(alpha = 0.4f),
            modifier           = Modifier.size(20.dp),
        )
    }
}
