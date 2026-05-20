package com.droidcon.global.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createAndroidDatabase(context: Context): ConferenceDatabase {
    val appContext = context.applicationContext
    val dbPath = appContext.getDatabasePath("conference.db").absolutePath
    return Room.databaseBuilder<ConferenceDatabase>(
        context = appContext,
        name = dbPath,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}
