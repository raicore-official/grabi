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

    fun summary(activeCount: Int, pausedCount: Int = 0): Notification {
        val text = if (activeCount > 0) {
            "Downloading $activeCount item${if (activeCount == 1) "" else "s"}"
        } else {
            "$pausedCount download${if (pausedCount == 1) "" else "s"} paused"
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Grabi")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.notif_open_app), openAppIntent())
        if (activeCount > 0) {
            builder.addAction(
                0,
                context.getString(R.string.notif_pause_all),
                bulkAction(DownloadService.ACTION_PAUSE_ALL),
            )
        }
        if (pausedCount > 0) {
            builder.addAction(
                0,
                context.getString(R.string.notif_resume_all),
                bulkAction(DownloadService.ACTION_RESUME_ALL),
            )
        }
        return builder.build()
    }

    fun updateSummary(activeCount: Int, pausedCount: Int = 0) {
        notifications.notify(DownloadService.SUMMARY_ID, summary(activeCount, pausedCount))
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

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun bulkAction(action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, DownloadService::class.java).setAction(action),
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