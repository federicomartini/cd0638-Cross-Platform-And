package com.droidcon.global.data.repository

import com.droidcon.global.data.local.ConferenceDatabase
import com.droidcon.global.data.local.entity.SessionEntity
import com.droidcon.global.data.local.entity.SpeakerEntity
import com.droidcon.global.data.remote.ConferenceApi
import com.droidcon.global.data.remote.model.SessionDto
import com.droidcon.global.data.remote.model.SpeakerDto
import com.droidcon.global.domain.model.Session
import com.droidcon.global.domain.model.Speaker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConferenceRepository(
    private val api: ConferenceApi,
    private val database: ConferenceDatabase
) {
    private val refreshMutex = Mutex()

    fun observeSessions(): Flow<List<Session>> =
        database.sessionDao().observeAll().map { sessions -> sessions.map { it.toDomain() } }

    fun observeSpeakers(): Flow<List<Speaker>> =
        database.speakerDao().observeAll().map { speakers -> speakers.map { it.toDomain() } }

    suspend fun refresh() {
        refreshMutex.withLock {
            val sessions = api.getSessions()
            database.sessionDao().insertAll(sessions.map { it.toEntity() })

            val allSpeakers = sessions.flatMap { session ->
                runCatching { api.getSpeakers(session.id) }
                    .getOrElse { emptyList() }
                    .map { speaker ->
                        speaker.copy(sessionId = speaker.sessionId.ifBlank { session.id })
                    }
            }
            database.speakerDao().insertAll(allSpeakers.map { it.toEntity() })
        }
    }
}

private fun SessionDto.toEntity(): SessionEntity =
    SessionEntity(
        id = id,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        room = room,
        speakerId = speakerId,
        isServiceSession = isServiceSession
    )

private fun SpeakerDto.toEntity(): SpeakerEntity =
    SpeakerEntity(
        id = id,
        name = name,
        bio = bio,
        avatar = avatar,
        company = company,
        sessionId = sessionId
    )

private fun SessionEntity.toDomain(): Session =
    Session(
        id = id,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        room = room,
        speakerId = speakerId,
        isServiceSession = isServiceSession
    )

private fun SpeakerEntity.toDomain(): Speaker =
    Speaker(
        id = id,
        name = name,
        bio = bio,
        avatar = avatar,
        company = company,
        sessionId = sessionId
    )
