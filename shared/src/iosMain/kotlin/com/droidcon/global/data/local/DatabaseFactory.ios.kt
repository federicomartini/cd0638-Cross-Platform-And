package com.droidcon.global.data.local

import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosDatabase(): ConferenceDatabase {
    val dbPath = iosDatabasePath()
    return Room.databaseBuilder<ConferenceDatabase>(name = dbPath)
        .setDriver(NativeSQLiteDriver())
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun iosDatabasePath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(directory?.path) + "/conference.db"
}
