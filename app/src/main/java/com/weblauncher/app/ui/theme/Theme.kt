package com.weblauncher.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Purple500,
    onPrimary        = Color.White,
    primaryContainer = Purple800,
    onPrimaryContainer = Purple100,

    secondary        = Purple400,
    onSecondary      = Color.White,
    secondaryContainer = Purple900,
    onSecondaryContainer = Purple200,

    background       = BackgroundDark,
    onBackground     = TextPrimary,

    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error            = AccentRed,
    onError          = Color.White,

    outline          = Color(0xFF3A3A5C),
)

@Composable
fun WebLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}
