package com.raisoft.grabi.ui.theme

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raisoft.grabi.data.settings.ThemeMode
import com.raisoft.grabi.ui.components.EmptyState
import com.raisoft.grabi.ui.components.GlassNavBar
import com.raisoft.grabi.ui.navigation.TopLevelDestination

@Preview(name = "Scheme - Light", showBackground = true)
@Composable
private fun SchemeLightPreview() {
    GrabiTheme(themeMode = ThemeMode.LIGHT) { SchemeSwatches() }
}

@Preview(name = "Scheme - Dark", showBackground = true)
@Composable
private fun SchemeDarkPreview() {
    GrabiTheme(themeMode = ThemeMode.DARK) { SchemeSwatches() }
}

@Preview(name = "Components - Light", showBackground = true)
@Composable
private fun ComponentsLightPreview() {
    GrabiTheme(themeMode = ThemeMode.LIGHT) { ComponentSample() }
}

@Preview(name = "Components - Dark", showBackground = true)
@Composable
private fun ComponentsDarkPreview() {
    GrabiTheme(themeMode = ThemeMode.DARK) { ComponentSample() }
}

@Composable
private fun SchemeSwatches() {
    val scheme = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "secondary" to scheme.secondary,
        "tertiary" to scheme.tertiary,
        "error" to scheme.error,
        "background" to scheme.background,
        "surface" to scheme.surface,
        "surfaceVariant" to scheme.surfaceVariant,
        "outline" to scheme.outline,
        "outlineVariant" to scheme.outlineVariant,
        "containerLowest" to scheme.surfaceContainerLowest,
        "containerLow" to scheme.surfaceContainerLow,
        "container" to scheme.surfaceContainer,
        "containerHigh" to scheme.surfaceContainerHigh,
        "containerHighest" to scheme.surfaceContainerHighest,
        "inverseSurface" to scheme.inverseSurface,
        "scrim" to scheme.scrim,
    )
    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Grabi scheme", style = MaterialTheme.typography.titleMedium)
            roles.forEach { (name, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentSample() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
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