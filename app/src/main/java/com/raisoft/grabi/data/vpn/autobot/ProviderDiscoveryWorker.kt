package com.raisoft.grabi.data.vpn.autobot

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors

class ProviderDiscoveryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, AutoBotEntryPoint::class.java)
        return if (entry.manager().runCycle(allowReplace = false)) Result.success() else Result.retry()
    }
}