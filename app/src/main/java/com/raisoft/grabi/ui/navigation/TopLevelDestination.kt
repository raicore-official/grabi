package com.raisoft.grabi.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.raisoft.grabi.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME("home", R.string.nav_home, R.drawable.ic_nav_home),
    BROWSER("browser", R.string.nav_browser, R.drawable.ic_nav_browser),
    DOWNLOADS("downloads", R.string.nav_downloads, R.drawable.ic_nav_downloads),
    LIBRARY("library", R.string.nav_library, R.drawable.ic_nav_library),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_nav_settings),
}