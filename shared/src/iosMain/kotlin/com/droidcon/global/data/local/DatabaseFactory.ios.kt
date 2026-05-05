package com.droidcon.global.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSHomeDirectory

fun createIosDatabase(): ConferenceDatabase {
    val dbPath = NSHomeDirectory() + "/conference.db"
    return Room.databaseBuilder<ConferenceDatabase>(
        name = dbPath,
        factory = { ConferenceDatabaseConstructor.initialize() }
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}
