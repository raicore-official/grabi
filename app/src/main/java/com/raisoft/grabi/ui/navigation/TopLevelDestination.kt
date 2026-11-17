package com.raisoft.grabi.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.raisoft.grabi.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    BROWSER("browser", R.string.nav_browser, Icons.Rounded.Language, Icons.Outlined.Language),
    DOWNLOADS("downloads", R.string.nav_downloads, Icons.Rounded.Download, Icons.Outlined.Download),
    LIBRARY("library", R.string.nav_library, Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
}