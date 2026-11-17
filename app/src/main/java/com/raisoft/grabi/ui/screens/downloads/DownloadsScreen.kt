package com.raisoft.grabi.ui.screens.downloads

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.raisoft.grabi.R
import com.raisoft.grabi.ui.components.EmptyState

@Composable
fun DownloadsScreen() {
    EmptyState(
        icon = Icons.Outlined.Download,
        title = stringResource(R.string.downloads_title),
        message = stringResource(R.string.downloads_message),
    )
}