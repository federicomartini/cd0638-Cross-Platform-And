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

    @Query("DELETE FROM speakers WHERE sessionId NOT IN (:sessionIds)")
    protected abstract suspend fun deleteSpeakersNotIn(sessionIds: List<String>)

    @Query("DELETE FROM speakers WHERE sessionId = :sessionId")
    protected abstract suspend fun deleteSpeakersForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSessions(items: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSpeakers(items: List<SpeakerEntity>)

    /** Replaces session rows; keeps speaker rows for sessions still present, drops orphans only. */
    @Transaction
    open suspend fun replaceSessions(sessions: List<SessionEntity>) {
        deleteAllSessions()
        if (sessions.isNotEmpty()) {
            insertSessions(sessions)
            deleteSpeakersNotIn(sessions.map { it.id })
        } else {
            deleteAllSpeakers()
        }
    }

    /** Replaces speakers for one session without touching other sessions or session rows. */
    @Transaction
    open suspend fun replaceSpeakersForSession(
        sessionId: String,
        speakers: List<SpeakerEntity>,
    ) {
        deleteSpeakersForSession(sessionId)
        if (speakers.isNotEmpty()) {
            insertSpeakers(speakers)
        }
    }
}
