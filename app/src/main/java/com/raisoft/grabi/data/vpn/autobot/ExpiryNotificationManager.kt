package com.raisoft.grabi.data.vpn.autobot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.raisoft.grabi.MainActivity
import com.raisoft.grabi.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notifications = NotificationManagerCompat.from(context)
    private val lastNotified = HashMap<String, Long>()

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "AutoBot automation", NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "VPN expiry alerts", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun renewalStarted(name: String) =
        post("renewal-start", CHANNEL_STATUS, ID_STATUS, "VPN renewal started", "Fetching $name", minIntervalMs = 0)

    fun configFound(name: String) =
        post("found", CHANNEL_STATUS, ID_STATUS, "New VPN configuration found", name)

    fun configValidated(name: String) =
        post("validated", CHANNEL_STATUS, ID_STATUS, "Configuration validated", name)

    fun renewalSuccess(name: String) =
        post("renewal-ok", CHANNEL_STATUS, ID_STATUS, "VPN renewal successful", "Connected via $name", minIntervalMs = 0)

    fun renewalFailed(reason: String) =
        post("renewal-fail", CHANNEL_ALERTS, ID_ALERT, "VPN renewal failed", reason.take(120), minIntervalMs = 0)

    fun expiring(hoursLeft: Long) =
        post("expiring", CHANNEL_ALERTS, ID_ALERT, "VPN configuration expiring", "About ${hoursLeft}h left — AutoBot will renew it")

    fun expired() =
        post("expired", CHANNEL_ALERTS, ID_ALERT, "VPN configuration expired", "AutoBot is looking for a replacement", minIntervalMs = 0)

    fun providerVerification(provider: String) =
        post(
            "verify-$provider",
            CHANNEL_ALERTS,
            ID_ALERT,
            "$provider needs your verification",
            "Open AutoBot to complete the provider check in a browser",
            route = "vpnbook-handoff",
        )

    private fun post(
        key: String,
        channel: String,
        id: Int,
        title: String,
        text: String,
        route: String = "autobot",
        minIntervalMs: Long = 6 * 3_600_000L,
    ) {
        if (!notifications.areNotificationsEnabled()) return
        val now = System.currentTimeMillis()
        synchronized(lastNotified) {
            val last = lastNotified[key]
            if (last != null && now - last < minIntervalMs) return
            lastNotified[key] = now
        }
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notifications.notify(
            id,
            NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build(),
        )
    }

    companion object {
        const val CHANNEL_STATUS = "grabi_autobot"
        const val CHANNEL_ALERTS = "grabi_autobot_alerts"
        const val ID_STATUS = 40
        const val ID_ALERT = 41
    }
}