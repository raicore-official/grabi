package com.raisoft.grabi.ui.screens.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.raisoft.grabi.R
import com.raisoft.grabi.ui.components.EmptyState

@Composable
fun BrowserScreen() {
    EmptyState(
        icon = Icons.Outlined.Language,
        title = stringResource(R.string.browser_title),
        message = stringResource(R.string.browser_message),
    )
}