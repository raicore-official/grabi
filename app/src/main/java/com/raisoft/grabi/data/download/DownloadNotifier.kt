package com.raisoft.grabi.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.raisoft.grabi.MainActivity
import com.raisoft.grabi.R
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.model.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notifications = context.getSystemService(NotificationManager::class.java)

    init {
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun progress(entity: DownloadEntity, percent: Float, speed: String?, eta: Long) {
        val parts = mutableListOf("${(percent * 100).toInt()}%")
        speed?.let { parts.add(it) }
        if (eta >= 0) parts.add("ETA ${formatDuration(eta.toInt())}")
        notifications.notify(
            entity.id.hashCode(),
            base(entity)
                .setContentText(parts.joinToString(" · "))
                .setProgress(100, (percent * 100).toInt(), false)
                .setOngoing(true)
                .addAction(0, "Pause", serviceAction(entity.id, DownloadService.ACTION_PAUSE))
                .addAction(0, "Cancel", serviceAction(entity.id, DownloadService.ACTION_CANCEL))
                .build(),
        )
    }

    fun done(entity: DownloadEntity, location: String) {
        notifications.notify(
            entity.id.hashCode(),
            base(entity)
                .setContentText("Saved · tap to open")
                .setAutoCancel(true)
                .setContentIntent(openIntent(entity.id, location))
                .addAction(0, "Open", openIntent(entity.id, location))
                .addAction(0, "Dismiss", serviceAction(entity.id, DownloadService.ACTION_DISMISS))
                .build(),
        )
    }

    fun failed(entity: DownloadEntity, error: String?) {
        notifications.notify(
            entity.id.hashCode(),
            base(entity)
                .setContentText(error?.take(120) ?: "Download failed")
                .addAction(0, "Retry", activityAction(entity.id, MainActivity.EXTRA_RETRY_ID))
                .addAction(0, "Dismiss", serviceAction(entity.id, DownloadService.ACTION_DISMISS))
                .build(),
        )
    }

    fun stopped(entity: DownloadEntity, status: DownloadStatus) {
        val builder = base(entity)
        if (status == DownloadStatus.PAUSED) {
            builder.setContentText("Paused")
                .addAction(0, "Resume", activityAction(entity.id, MainActivity.EXTRA_RESUME_ID))
        } else {
            builder.setContentText("Canceled")
                .addAction(0, "Retry", activityAction(entity.id, MainActivity.EXTRA_RETRY_ID))
        }
        notifications.notify(
            entity.id.hashCode(),
            builder.addAction(0, "Dismiss", serviceAction(entity.id, DownloadService.ACTION_DISMISS)).build(),
        )
    }

    fun summary(activeCount: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Grabi")
            .setContentText("Downloading $activeCount item${if (activeCount == 1) "" else "s"}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun updateSummary(activeCount: Int) {
        notifications.notify(DownloadService.SUMMARY_ID, summary(activeCount))
    }

    fun cancel(id: String) {
        notifications.cancel(id.hashCode())
    }

    private fun base(entity: DownloadEntity) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(entity.title)
            .setOnlyAlertOnce(true)

    private fun serviceAction(id: String, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            id.hashCode() xor action.hashCode(),
            Intent(context, DownloadService::class.java).setAction(action).putExtra(DownloadService.EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun activityAction(id: String, extraKey: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            id.hashCode() xor extraKey.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(extraKey, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openIntent(id: String, location: String): PendingIntent {
        val uri = if (location.startsWith("content:")) {
            Uri.parse(location)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(location))
        }
        return PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "grabi_downloads"
    }
}