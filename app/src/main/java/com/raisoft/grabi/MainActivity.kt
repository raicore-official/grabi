package com.raisoft.grabi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.download.PendingLink
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.data.settings.ThemeMode
import com.raisoft.grabi.domain.LinkValidator
import com.raisoft.grabi.ui.GrabiApp
import com.raisoft.grabi.ui.theme.GrabiTheme
import com.raisoft.grabi.ui.theme.SystemBarSync
import com.raisoft.grabi.ui.theme.resolveDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var pendingLink: PendingLink
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var settingsRepository: SettingsRepository

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Read the persisted theme before the first frame so the window never flashes.
        val initial = runBlocking { SettingsRepository(applicationContext).settings.first() }
        AppCompatDelegate.setDefaultNightMode(initial.themeMode.toNightMode())
        super.onCreate(savedInstanceState)
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val initialDark = initial.themeMode.resolveDark(systemDark)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { initialDark },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { initialDark },
        )
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        var themeMode by mutableStateOf(initial.themeMode)
        var dynamicColor by mutableStateOf(initial.dynamicColor)
        lifecycleScope.launch {
            settingsRepository.settings.collect { settings ->
                themeMode = settings.themeMode
                dynamicColor = settings.dynamicColor
                AppCompatDelegate.setDefaultNightMode(settings.themeMode.toNightMode())
            }
        }
        setContent {
            GrabiTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
            ) {
                SystemBarSync(themeMode.resolveDark(isSystemInDarkTheme()))
                GrabiApp(pendingLink = pendingLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        downloadManager.pump()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        intent.getStringExtra(EXTRA_RETRY_ID)?.let(downloadManager::retry)
        intent.getStringExtra(EXTRA_RESUME_ID)?.let(downloadManager::resume)
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            LinkValidator.extractUrl(text)?.let { pendingLink.offer(it) }
        }
    }

    companion object {
        const val EXTRA_RETRY_ID = "grabi_retry_id"
        const val EXTRA_RESUME_ID = "grabi_resume_id"
    }
}

private fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
}