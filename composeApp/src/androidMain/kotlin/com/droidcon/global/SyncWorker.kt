package com.droidcon.global

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.droidcon.global.data.local.createAndroidDatabase
import com.droidcon.global.data.remote.ConferenceApiException
import kotlinx.coroutines.CancellationException

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SharedGraph.initIfNeeded { createAndroidDatabase(applicationContext) }
        return try {
            SharedGraph.repository.refresh()
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ConferenceApiException) {
            Result.failure()
        } catch (error: Exception) {
            Result.retry()
        }
    }
}
