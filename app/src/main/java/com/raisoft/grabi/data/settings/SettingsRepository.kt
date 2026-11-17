package com.raisoft.grabi.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val parallelismKey = intPreferencesKey("download_parallelism")
    private val wifiOnlyKey = booleanPreferencesKey("wifi_only")

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { preferences ->
        UserSettings(
            themeMode = preferences[themeModeKey]
                ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
                ?: ThemeMode.DARK,
            dynamicColor = preferences[dynamicColorKey] ?: false,
            downloadParallelism = (preferences[parallelismKey] ?: 2).coerceIn(1, 3),
            wifiOnly = preferences[wifiOnlyKey] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setDownloadParallelism(value: Int) {
        context.settingsDataStore.edit { it[parallelismKey] = value.coerceIn(1, 3) }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[wifiOnlyKey] = enabled }
    }
}