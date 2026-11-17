package com.raisoft.grabi.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raisoft.grabi.data.download.PendingLink
import com.raisoft.grabi.ui.components.GlassNavBar
import com.raisoft.grabi.ui.navigation.TopLevelDestination
import com.raisoft.grabi.ui.screens.browser.BrowserScreen
import com.raisoft.grabi.ui.screens.downloads.DownloadsScreen
import com.raisoft.grabi.ui.screens.grab.GrabScreen
import com.raisoft.grabi.ui.screens.home.HomeScreen
import com.raisoft.grabi.ui.screens.library.LibraryScreen
import com.raisoft.grabi.ui.screens.player.PlayerScreen
import com.raisoft.grabi.ui.screens.settings.SettingsScreen
import com.raisoft.grabi.ui.screens.vpn.VpnScreen

@Composable
fun GrabiApp(pendingLink: PendingLink) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute?.startsWith("player") != true && currentRoute != "vpn"

    fun openPlayer(url: String, title: String, thumb: String = "") {
        navController.navigate(
            "player?url=${Uri.encode(url)}&title=${Uri.encode(title)}&thumb=${Uri.encode(thumb)}",
        )
    }

    fun openGrab() {
        navController.navigate(TopLevelDestination.GRAB.route) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(navController) {
        pendingLink.url.collect { url ->
            if (url != null) openGrab()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(220)) },
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onOpenPlayer = ::openPlayer,
                    onOpenBrowser = {
                        navController.navigate(TopLevelDestination.BROWSER.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGrab = { url ->
                        pendingLink.offer(url)
                        openGrab()
                    },
                )
            }
            composable(TopLevelDestination.GRAB.route) {
                GrabScreen()
            }
            composable(TopLevelDestination.BROWSER.route) {
                BrowserScreen(
                    onDownloadPage = { url ->
                        pendingLink.offer(url)
                        openGrab()
                    },
                )
            }
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
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(onOpenVpn = { navController.navigate("vpn") })
            }
            composable("vpn") {
                VpnScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "player?url={url}&title={title}&thumb={thumb}",
                arguments = listOf(
                    navArgument("url") { defaultValue = "" },
                    navArgument("title") { defaultValue = "" },
                    navArgument("thumb") { defaultValue = "" },
                ),
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { u, t -> openPlayer(u, t) },
                )
            }
        }
        if (showBottomBar) {
            GlassNavBar(
                currentRoute = currentRoute,
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}