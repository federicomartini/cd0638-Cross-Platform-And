package com.droidcon.global.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.droidcon.global.data.local.dao.ConferenceSyncDao
import com.droidcon.global.data.local.dao.SessionDao
import com.droidcon.global.data.local.dao.SpeakerDao
import com.droidcon.global.data.local.entity.SessionEntity
import com.droidcon.global.data.local.entity.SpeakerEntity

@Database(
    entities = [SessionEntity::class, SpeakerEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ConferenceDatabaseConstructor::class)
abstract class ConferenceDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun speakerDao(): SpeakerDao
    abstract fun conferenceSyncDao(): ConferenceSyncDao
}

// Room KSP generates platform `actual` implementations for non-Android targets.
@Suppress("KotlinNoActualForExpect")
expect object ConferenceDatabaseConstructor : RoomDatabaseConstructor<ConferenceDatabase> {
    override fun initialize(): ConferenceDatabase
}
