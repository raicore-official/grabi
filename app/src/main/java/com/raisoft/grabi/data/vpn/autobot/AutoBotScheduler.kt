package com.raisoft.grabi.data.vpn.autobot

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.raisoft.grabi.domain.vpn.ExpiryCalculator
import java.util.concurrent.TimeUnit

object AutoBotScheduler {
    const val DISCOVERY_NAME = "grabi-autobot-discovery"
    const val RENEWAL_NAME = "grabi-autobot-renewal"
    const val EXPIRY_NAME = "grabi-autobot-expiry"
    const val DISCOVERY_INTERVAL_HOURS = 6L

    fun sync(context: Context, enabled: Boolean, nextRenewalAt: Long, expiresAt: Long?) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(DISCOVERY_NAME)
            workManager.cancelUniqueWork(RENEWAL_NAME)
            workManager.cancelUniqueWork(EXPIRY_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val discovery = PeriodicWorkRequestBuilder<ProviderDiscoveryWorker>(
            DISCOVERY_INTERVAL_HOURS, TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(DISCOVERY_NAME, ExistingPeriodicWorkPolicy.KEEP, discovery)

        val now = System.currentTimeMillis()
        val renewal = OneTimeWorkRequestBuilder<ConfigRenewalWorker>()
            .setInitialDelay((nextRenewalAt - now).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(RENEWAL_NAME, ExistingWorkPolicy.REPLACE, renewal)

        if (expiresAt != null && expiresAt > 0) {
            val checkDelay = (expiresAt - ExpiryCalculator.EXPIRING_SOON_MS - now).coerceAtLeast(0L)
            val check = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
                .setInitialDelay(checkDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(EXPIRY_NAME, ExistingWorkPolicy.REPLACE, check)
        } else {
            workManager.cancelUniqueWork(EXPIRY_NAME)
        }
    }
}