package com.raisoft.grabi.data.download

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.raisoft.grabi.domain.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var manager: DownloadManager
    @Inject lateinit var notifier: DownloadNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            SUMMARY_ID,
            notifier.summary(1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        val id = intent?.getStringExtra(EXTRA_ID)
        when (intent?.action) {
            ACTION_PAUSE -> id?.let(manager::pause)
            ACTION_CANCEL -> id?.let(manager::cancel)
            ACTION_DISMISS -> id?.let(notifier::cancel)
        }
        observeQueue()
        return START_STICKY
    }

    private fun observeQueue() {
        if (observing) return
        observing = true
        scope.launch {
            manager.downloads.collect { list ->
                val active = list.count {
                    it.status == DownloadStatus.RUNNING.name || it.status == DownloadStatus.QUEUED.name
                }
                if (active == 0) {
                    observing = false
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notifier.updateSummary(active)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val SUMMARY_ID = 1
        const val EXTRA_ID = "id"
        const val ACTION_PAUSE = "com.raisoft.grabi.action.PAUSE"
        const val ACTION_CANCEL = "com.raisoft.grabi.action.CANCEL"
        const val ACTION_DISMISS = "com.raisoft.grabi.action.DISMISS"
    }
}