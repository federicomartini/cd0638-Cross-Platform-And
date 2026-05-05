package com.droidcon.global.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.droidcon.global.data.local.entity.SpeakerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeakerDao {
    @Query("SELECT * FROM speakers WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: String): Flow<List<SpeakerEntity>>

    @Query("SELECT * FROM speakers")
    fun observeAll(): Flow<List<SpeakerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SpeakerEntity>)
}
