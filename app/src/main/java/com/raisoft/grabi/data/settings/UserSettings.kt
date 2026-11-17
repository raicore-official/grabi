package com.raisoft.grabi.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val downloadParallelism: Int = 2,
    val wifiOnly: Boolean = false,
)