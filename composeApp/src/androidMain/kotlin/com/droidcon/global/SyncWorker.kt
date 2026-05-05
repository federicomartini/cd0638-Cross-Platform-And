package com.droidcon.global

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.droidcon.global.data.local.createAndroidDatabase

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SharedGraph.initIfNeeded { createAndroidDatabase(applicationContext) }
        return runCatching {
            SharedGraph.repository.refresh()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
