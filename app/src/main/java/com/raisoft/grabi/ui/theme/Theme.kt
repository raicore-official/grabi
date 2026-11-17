package com.raisoft.grabi.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.raisoft.grabi.data.settings.ThemeMode

fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val GrabiLightColors = lightColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color(0xFF3A0B01),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B01),
    inversePrimary = Color(0xFFFFB59D),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF2C150F),
    tertiary = Color(0xFF6C5D2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E1A7),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFF5F0E8),
    onBackground = Color(0xFF201B16),
    surface = Color(0xFFF5F0E8),
    onSurface = Color(0xFF201B16),
    surfaceVariant = Color(0xFFF0E0D6),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceTint = Color(0xFFD97757),
    inverseSurface = Color(0xFF362F2A),
    inverseOnSurface = Color(0xFFFBEEE6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF85746B),
    outlineVariant = Color(0xFFD8C3B8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F4),
    surfaceDim = Color(0xFFE4DCD3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF3EA),
    surfaceContainer = Color(0xFFF2EBE1),
    surfaceContainerHigh = Color(0xFFECE5DB),
    surfaceContainerHighest = Color(0xFFE6DFD5),
)

private val GrabiDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF56200C),
    primaryContainer = Color(0xFF7A341E),
    onPrimaryContainer = Color(0xFFFFDBCF),
    inversePrimary = Color(0xFFD97757),
    secondary = Color(0xFFE7BDB2),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFD8C68D),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534619),
    onTertiaryContainer = Color(0xFFF5E1A7),
    background = Color(0xFF1F1D1B),
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF1F1D1B),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD8C3B8),
    surfaceTint = Color(0xFFFFB59D),
    inverseSurface = Color(0xFFEDE0D9),
    inverseOnSurface = Color(0xFF362F2A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF52443C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF423B36),
    surfaceDim = Color(0xFF1A1613),
    surfaceContainerLowest = Color(0xFF171411),
    surfaceContainerLow = Color(0xFF231F1C),
    surfaceContainer = Color(0xFF282320),
    surfaceContainerHigh = Color(0xFF332D29),
    surfaceContainerHighest = Color(0xFF3E3733),
)

// Over-media roles: intentionally identical in light and dark, defined only here.
val ColorScheme.onVideo: Color get() = VideoWhite
val ColorScheme.videoScrim: Color get() = VideoBlack
val ColorScheme.liveBadge: Color get() = LiveRed
val ColorScheme.onLiveBadge: Color get() = VideoWhite
val ColorScheme.liveText: Color get() = LiveRedSoft

@Composable
fun GrabiTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.resolveDark(isSystemInDarkTheme())
    val target = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GrabiDarkColors
        else -> GrabiLightColors
    }
    MaterialTheme(
        colorScheme = animateScheme(target),
        typography = GrabiTypography,
        content = content,
    )
}

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = anim(target.primary),
    onPrimary = anim(target.onPrimary),
    primaryContainer = anim(target.primaryContainer),
    onPrimaryContainer = anim(target.onPrimaryContainer),
    inversePrimary = anim(target.inversePrimary),
    secondary = anim(target.secondary),
    onSecondary = anim(target.onSecondary),
    secondaryContainer = anim(target.secondaryContainer),
    onSecondaryContainer = anim(target.onSecondaryContainer),
    tertiary = anim(target.tertiary),
    onTertiary = anim(target.onTertiary),
    tertiaryContainer = anim(target.tertiaryContainer),
    onTertiaryContainer = anim(target.onTertiaryContainer),
    background = anim(target.background),
    onBackground = anim(target.onBackground),
    surface = anim(target.surface),
    onSurface = anim(target.onSurface),
    surfaceVariant = anim(target.surfaceVariant),
    onSurfaceVariant = anim(target.onSurfaceVariant),
    surfaceTint = anim(target.surfaceTint),
    inverseSurface = anim(target.inverseSurface),
    inverseOnSurface = anim(target.inverseOnSurface),
    error = anim(target.error),
    onError = anim(target.onError),
    errorContainer = anim(target.errorContainer),
    onErrorContainer = anim(target.onErrorContainer),
    outline = anim(target.outline),
    outlineVariant = anim(target.outlineVariant),
    scrim = anim(target.scrim),
    surfaceBright = anim(target.surfaceBright),
    surfaceDim = anim(target.surfaceDim),
    surfaceContainerLowest = anim(target.surfaceContainerLowest),
    surfaceContainerLow = anim(target.surfaceContainerLow),
    surfaceContainer = anim(target.surfaceContainer),
    surfaceContainerHigh = anim(target.surfaceContainerHigh),
    surfaceContainerHighest = anim(target.surfaceContainerHighest),
)

@Composable
private fun anim(target: Color): Color =
    animateColorAsState(target, tween(200), label = "grabiTheme").value