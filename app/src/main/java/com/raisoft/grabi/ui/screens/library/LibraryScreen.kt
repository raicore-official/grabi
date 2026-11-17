package com.raisoft.grabi.ui.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.raisoft.grabi.R
import com.raisoft.grabi.ui.components.EmptyState

@Composable
fun LibraryScreen() {
    EmptyState(
        icon = Icons.Outlined.VideoLibrary,
        title = stringResource(R.string.library_title),
        message = stringResource(R.string.library_message),
    )
}