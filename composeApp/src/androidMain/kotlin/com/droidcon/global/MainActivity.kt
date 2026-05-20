package com.droidcon.global

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.droidcon.global.data.local.createAndroidDatabase
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SharedGraph.initIfNeeded { createAndroidDatabase(applicationContext) }

        setContent {
            App()
        }

        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val wm = WorkManager.getInstance(applicationContext)

        if (savedInstanceState == null) {
            val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(syncConstraints)
                .build()
            wm.enqueueUniqueWork(
                "conference_sync_immediate",
                ExistingWorkPolicy.KEEP,
                immediateSync
            )
        }

        val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()
        wm.enqueueUniquePeriodicWork(
            "conference_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSync
        )
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}