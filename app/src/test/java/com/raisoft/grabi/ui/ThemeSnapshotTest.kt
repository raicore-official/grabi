package com.raisoft.grabi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.raisoft.grabi.data.settings.ThemeMode
import com.raisoft.grabi.ui.components.EmptyState
import com.raisoft.grabi.ui.components.GlassNavBar
import com.raisoft.grabi.ui.navigation.TopLevelDestination
import com.raisoft.grabi.ui.theme.GrabiTheme
import org.junit.Rule
import org.junit.Test

class ThemeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun schemeBoardLight() {
        paparazzi.snapshot {
            GrabiTheme(themeMode = ThemeMode.LIGHT) { SchemeBoard() }
        }
    }

    @Test
    fun schemeBoardDark() {
        paparazzi.snapshot {
            GrabiTheme(themeMode = ThemeMode.DARK) { SchemeBoard() }
        }
    }

    @Test
    fun componentsLight() {
        paparazzi.snapshot {
            GrabiTheme(themeMode = ThemeMode.LIGHT) { ComponentBoard() }
        }
    }

    @Test
    fun componentsDark() {
        paparazzi.snapshot {
            GrabiTheme(themeMode = ThemeMode.DARK) { ComponentBoard() }
        }
    }
}

@Composable
private fun SchemeBoard() {
    val scheme = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "onPrimaryContainer" to scheme.onPrimaryContainer,
        "secondary" to scheme.secondary,
        "onSecondary" to scheme.onSecondary,
        "tertiary" to scheme.tertiary,
        "error" to scheme.error,
        "onError" to scheme.onError,
        "background" to scheme.background,
        "onBackground" to scheme.onBackground,
        "surface" to scheme.surface,
        "onSurface" to scheme.onSurface,
        "surfaceVariant" to scheme.surfaceVariant,
        "onSurfaceVariant" to scheme.onSurfaceVariant,
        "outline" to scheme.outline,
        "outlineVariant" to scheme.outlineVariant,
        "containerLowest" to scheme.surfaceContainerLowest,
        "containerLow" to scheme.surfaceContainerLow,
        "container" to scheme.surfaceContainer,
        "containerHigh" to scheme.surfaceContainerHigh,
        "containerHighest" to scheme.surfaceContainerHighest,
        "inverseSurface" to scheme.inverseSurface,
        "inverseOnSurface" to scheme.inverseOnSurface,
        "inversePrimary" to scheme.inversePrimary,
        "scrim" to scheme.scrim,
    )
    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Grabi color scheme", style = MaterialTheme.typography.titleMedium)
            roles.forEach { (name, color) -> RoleRow(name, color) }
        }
    }
}

@Composable
private fun RoleRow(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ComponentBoard() {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Rounded.Download,
                title = "Nothing here yet",
                message = "Grabbed videos and audio will appear in this library.",
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            GlassNavBar(
                currentRoute = TopLevelDestination.HOME.route,
                onSelect = {},
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}