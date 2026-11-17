package com.raisoft.grabi.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class QueueRecoveryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, RecoveryEntryPoint::class.java)
        entry.manager().recoverAndPump()
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecoveryEntryPoint {
    fun manager(): DownloadManager
}