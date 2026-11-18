package com.raisoft.grabi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raisoft.grabi.ui.navigation.TopLevelDestination

@Composable
fun GlassNavBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainer.copy(alpha = 0.96f),
                        scheme.surface.copy(alpha = 0.96f),
                    ),
                ),
            )
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.12f), shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (selected) scheme.primaryContainer else Color.Transparent,
                    )
                    .clickable { onSelect(destination) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = stringResource(destination.labelRes),
                    tint = if (selected) {
                        scheme.onPrimaryContainer
                    } else {
                        scheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}