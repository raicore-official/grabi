package com.raisoft.grabi

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.raisoft.grabi.data.crash.CrashHandler
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.download.QueueRecoveryWorker
import com.raisoft.grabi.data.download.YtDlpEngine
import com.raisoft.grabi.data.playback.DebugLog
import com.raisoft.grabi.data.playback.OkHttpDownloader
import com.raisoft.grabi.data.vpn.VpnRepository
import com.raisoft.grabi.data.vpn.autobot.AutoBotManager
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GrabiApplication : Application() {

    @Inject lateinit var engine: YtDlpEngine
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var okHttpDownloader: OkHttpDownloader

    // Created eagerly so the always-on VPN callback is registered at process start.
    @Inject lateinit var vpnRepository: VpnRepository

    @Inject lateinit var autoBotManager: AutoBotManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        DebugLog.init(this)
        NewPipe.init(okHttpDownloader)
        applicationScope.launch { runCatching { engine.warmUp() } }
        applicationScope.launch { downloadManager.recoverAndPump() }
        applicationScope.launch { runCatching { autoBotManager.onAppStart() } }
        WorkManager.getInstance(this).enqueueUniqueWork(
            "grabi-queue-recovery",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<QueueRecoveryWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build(),
        )
    }
}