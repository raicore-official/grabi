package com.raisoft.grabi

import android.app.Application
import com.raisoft.grabi.data.download.YtDlpEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GrabiApplication : Application() {

    @Inject lateinit var engine: YtDlpEngine

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runCatching { engine.warmUp() } }
    }
}