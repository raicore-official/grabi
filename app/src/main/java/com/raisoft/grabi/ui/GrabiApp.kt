package com.raisoft.grabi.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raisoft.grabi.ui.navigation.TopLevelDestination
import com.raisoft.grabi.ui.screens.browser.BrowserScreen
import com.raisoft.grabi.ui.screens.downloads.DownloadsScreen
import com.raisoft.grabi.ui.screens.home.HomeScreen
import com.raisoft.grabi.ui.screens.library.LibraryScreen
import com.raisoft.grabi.ui.screens.player.PlayerScreen
import com.raisoft.grabi.ui.screens.settings.SettingsScreen

@Composable
fun GrabiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute?.startsWith("player") != true

    fun openPlayer(url: String, title: String) {
        navController.navigate("player?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = stringResource(destination.labelRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(220)) },
        ) {
            composable(TopLevelDestination.HOME.route) { HomeScreen() }
            composable(TopLevelDestination.BROWSER.route) { BrowserScreen() }
            composable(TopLevelDestination.DOWNLOADS.route) {
                DownloadsScreen(
                    onPlay = { entity ->
                        val target = entity.contentUri ?: entity.filePath
                        if (target != null) openPlayer(target, entity.title)
                    },
                )
            }
            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(onOpenPlayer = ::openPlayer)
            }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
            composable(
                route = "player?url={url}&title={title}",
                arguments = listOf(
                    navArgument("url") { defaultValue = "" },
                    navArgument("title") { defaultValue = "" },
                ),
            ) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}