package com.raisoft.grabi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.raisoft.grabi.data.settings.ThemeMode

private val GrabiDarkColors = darkColorScheme(
    primary = Ember500,
    onPrimary = Charcoal900,
    primaryContainer = Ember700,
    onPrimaryContainer = Paper,
    secondary = Smoke,
    onSecondary = Charcoal900,
    secondaryContainer = Ink600,
    onSecondaryContainer = Paper,
    background = Charcoal900,
    onBackground = Paper,
    surface = Ink950,
    onSurface = Paper,
    surfaceVariant = Ink800,
    onSurfaceVariant = Smoke,
    surfaceContainer = Ink800,
    surfaceContainerHigh = Ink600,
    outline = Smoke,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val GrabiLightColors = lightColorScheme(
    primary = Ember700,
    onPrimary = Paper,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF5A5F6B),
    onSecondary = Paper,
    background = Color(0xFFF7F7F9),
    onBackground = Charcoal900,
    surface = Paper,
    onSurface = Charcoal900,
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF444A55),
    outline = Color(0xFF757C89),
)

@Composable
fun GrabiTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GrabiDarkColors
        else -> GrabiLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GrabiTypography,
        content = content,
    )
}