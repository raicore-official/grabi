package com.raisoft.grabi.data.vpn.autobot

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class AutoBotWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, AutoBotEntryPoint::class.java)
        return if (entry.autoBotManager().runCycle()) Result.success() else Result.retry()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoBotEntryPoint {
    fun autoBotManager(): AutoBotManager
}