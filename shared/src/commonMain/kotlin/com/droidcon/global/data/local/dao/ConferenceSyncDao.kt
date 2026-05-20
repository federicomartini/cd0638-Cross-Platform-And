package com.droidcon.global.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.droidcon.global.data.local.entity.SessionEntity
import com.droidcon.global.data.local.entity.SpeakerEntity

@Dao
abstract class ConferenceSyncDao {
    @Query("DELETE FROM sessions")
    protected abstract suspend fun deleteAllSessions()

    @Query("DELETE FROM speakers")
    protected abstract suspend fun deleteAllSpeakers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSessions(items: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSpeakers(items: List<SpeakerEntity>)

    @Transaction
    open suspend fun replaceAll(
        sessions: List<SessionEntity>,
        speakers: List<SpeakerEntity>
    ) {
        deleteAllSessions()
        deleteAllSpeakers()
        insertSessions(sessions)
        insertSpeakers(speakers)
    }
}
