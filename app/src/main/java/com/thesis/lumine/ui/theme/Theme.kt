package com.thesis.lumine.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light colour scheme — crimson primary, gold secondary ────────────────────
private val LightColorScheme = lightColorScheme(
    primary               = Crimson40,
    onPrimary             = Color.White,
    primaryContainer      = Gold90,
    onPrimaryContainer    = Crimson10,
    secondary             = Gold40,
    onSecondary           = Color.White,
    secondaryContainer    = Gold90,
    onSecondaryContainer  = Gold10,
    tertiary              = NeutralRed40,
    onTertiary            = Color.White,
    tertiaryContainer     = NeutralRed90,
    onTertiaryContainer   = NeutralRed10,
    error                 = Color(0xFFBA1A1A),
    onError               = Color.White,
    errorContainer        = Color(0xFFFFDAD6),
    onErrorContainer      = Color(0xFF410002),
    background            = NeutralRed99,
    onBackground          = NeutralRed10,
    surface               = NeutralRed99,
    onSurface             = NeutralRed10,
    surfaceVariant        = NeutralVar90,
    onSurfaceVariant      = NeutralVar30,
    outline               = NeutralVar50,
    outlineVariant        = NeutralVar80,
    scrim                 = Color.Black,
    inverseSurface        = NeutralRed10,
    inverseOnSurface      = NeutralRed90,
    inversePrimary        = Gold80,
)

// ── Dark colour scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary               = Gold80,       // rich antique gold on dark — no more pink
    onPrimary             = Gold10,
    primaryContainer      = Crimson40,    // deep maroon container
    onPrimaryContainer    = Crimson90,
    secondary             = Gold60,
    onSecondary           = Gold10,
    secondaryContainer    = Gold20,
    onSecondaryContainer  = Gold80,
    tertiary              = NeutralRed80,
    onTertiary            = NeutralRed10,
    tertiaryContainer     = NeutralRed40,
    onTertiaryContainer   = NeutralRed90,
    error                 = Color(0xFFFFB4AB),
    onError               = Color(0xFF690005),
    errorContainer        = Color(0xFF93000A),
    onErrorContainer      = Color(0xFFFFDAD6),
    background            = NeutralRed10,
    onBackground          = NeutralRed90,
    surface               = Color(0xFF201714),
    onSurface             = NeutralRed80,
    surfaceVariant        = NeutralVar30,
    onSurfaceVariant      = NeutralVar80,
    outline               = NeutralVar60,
    outlineVariant        = NeutralVar30,
    scrim                 = Color.Black,
    inverseSurface        = NeutralRed90,
    inverseOnSurface      = NeutralRed10,
    inversePrimary        = Crimson40,
)

@Composable
fun LumineAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color disabled — use the JRP-inspired brand palette instead
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
