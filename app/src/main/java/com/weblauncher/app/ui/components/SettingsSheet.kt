package com.weblauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weblauncher.app.ui.theme.*

/**
 * Full-screen Compose settings dialog.
 *
 * Sections:
 *  1. General — homepage URL, ad blocking toggle
 *  2. Ad Blocking — custom blocklist textarea
 *  3. Browser Identity — user-agent toggle + string
 *  4. Zoom — default zoom slider, per-domain zoom memory toggle
 *  5. Data — clear cache & cookies
 */
@Composable
fun SettingsSheet(
    initialSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Local mutable copies of settings
    var homepage        by remember { mutableStateOf(initialSettings.homepageUrl) }
    var adBlocking      by remember { mutableStateOf(initialSettings.adBlockingEnabled) }
    var customBlocklist by remember { mutableStateOf(initialSettings.customBlocklist) }
    var uaEnabled       by remember { mutableStateOf(initialSettings.userAgentEnabled) }
    var uaString        by remember { mutableStateOf(initialSettings.userAgentOverride) }
    var domainZoom      by remember { mutableStateOf(initialSettings.domainZoomEnabled) }
    var defaultZoom     by remember { mutableFloatStateOf(initialSettings.defaultZoom.toFloat()) }
    var darkModeEnabled by remember { mutableStateOf(initialSettings.darkModeEnabled) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .padding(20.dp),
            ) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Settings",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        modifier   = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = SurfaceElevated, modifier = Modifier.padding(vertical = 12.dp))

                // ── Scrollable content ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // General
                    SettingsSection(title = "General", icon = Icons.Default.Language) {
                        SettingsTextField(
                            label       = "Homepage URL",
                            value       = homepage,
                            onValueChange = { homepage = it },
                            placeholder = "https://asurascans.com",
                            keyboardType = KeyboardType.Uri,
                        )
                        SettingsToggleRow(
                            label    = "Ad Blocking",
                            subtitle = "Block ads, trackers, and popups",
                            checked  = adBlocking,
                            icon     = Icons.Default.Shield,
                            onCheckedChange = { adBlocking = it },
                        )
                        SettingsToggleRow(
                            label    = "Force Dark Mode",
                            subtitle = "Invert colors for a dark theme (like DarkReader)",
                            checked  = darkModeEnabled,
                            icon     = Icons.Default.DarkMode,
                            onCheckedChange = { darkModeEnabled = it },
                        )
                    }

                    // Ad Blocking — custom rules
                    SettingsSection(title = "Custom Blocklist", icon = Icons.Default.Block) {
                        SettingsTextField(
                            label       = "Domains / Patterns",
                            value       = customBlocklist,
                            onValueChange = { customBlocklist = it },
                            placeholder = "example.com, ad-network.net, /popup/",
                            minLines    = 3,
                        )
                        Text(
                            "Comma-separated. Domains, substrings, or URL path patterns.",
                            fontSize = 11.sp,
                            color    = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    // User agent
                    SettingsSection(title = "Browser Identity", icon = Icons.Default.Fingerprint) {
                        SettingsToggleRow(
                            label    = "Spoof User Agent",
                            subtitle = "Appear as Chrome on Android",
                            checked  = uaEnabled,
                            icon     = Icons.Default.Android,
                            onCheckedChange = { uaEnabled = it },
                        )
                        if (uaEnabled) {
                            SettingsTextField(
                                label       = "User Agent String",
                                value       = uaString,
                                onValueChange = { uaString = it },
                                placeholder = "Mozilla/5.0 ...",
                                minLines    = 2,
                            )
                            TextButton(
                                onClick = {
                                    uaString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/125.0.0.0 Mobile Safari/537.36"
                                },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Reset to Chrome Default",
                                    color = Purple400, fontSize = 12.sp)
                            }
                        }
                    }

                    // Zoom
                    SettingsSection(title = "Zoom", icon = Icons.Default.ZoomIn) {
                        SettingsToggleRow(
                            label    = "Remember Zoom per Site",
                            subtitle = "Save zoom level for each domain",
                            checked  = domainZoom,
                            icon     = Icons.Default.Bookmarks,
                            onCheckedChange = { domainZoom = it },
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Default Zoom", color = TextPrimary, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            Text("${defaultZoom.toInt()}%", color = Purple400,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value         = defaultZoom,
                            onValueChange = { defaultZoom = it },
                            valueRange    = 50f..200f,
                            steps         = 29,
                            colors        = SliderDefaults.colors(
                                thumbColor       = Purple500,
                                activeTrackColor = Purple500,
                                inactiveTrackColor = SurfaceElevated,
                            ),
                        )
                    }

                    // Data management
                    SettingsSection(title = "Data", icon = Icons.Default.DeleteSweep) {
                        OutlinedButton(
                            onClick = onClearCache,
                            colors  = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentRed,
                            ),
                            border  = androidx.compose.foundation.BorderStroke(
                                width = androidx.compose.ui.unit.Dp(1f),
                                brush = androidx.compose.ui.graphics.SolidColor(AccentRed.copy(alpha = 0.5f)),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.DeleteForever, null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Cache & Cookies")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Save / Cancel ─────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    ) { Text("Cancel") }

                    Button(
                        onClick  = {
                            onSave(
                                AppSettings(
                                    homepageUrl       = homepage.trim().ifBlank { "https://asurascans.com" },
                                    adBlockingEnabled = adBlocking,
                                    customBlocklist   = customBlocklist,
                                    userAgentEnabled  = uaEnabled,
                                    userAgentOverride = uaString.trim(),
                                    domainZoomEnabled = domainZoom,
                                    defaultZoom       = defaultZoom.toInt(),
                                    darkModeEnabled   = darkModeEnabled,
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Purple500),
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable section container
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple400, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = Purple400, letterSpacing = 0.8.sp)
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable toggle row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = Purple500,
                uncheckedTrackColor = SurfaceElevated,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable text field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label, fontSize = 12.sp) },
        placeholder     = { Text(placeholder, fontSize = 12.sp, color = TextSecondary) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines        = minLines,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Purple500,
            unfocusedBorderColor = SurfaceElevated,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            focusedLabelColor    = Purple400,
            unfocusedLabelColor  = TextSecondary,
        ),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Data class (mirrors SettingsDialog.Settings)
// ─────────────────────────────────────────────────────────────────────────────

data class AppSettings(
    val homepageUrl: String,
    val adBlockingEnabled: Boolean,
    val customBlocklist: String,
    val userAgentEnabled: Boolean,
    val userAgentOverride: String,
    val domainZoomEnabled: Boolean,
    val defaultZoom: Int,
    val darkModeEnabled: Boolean,
)
