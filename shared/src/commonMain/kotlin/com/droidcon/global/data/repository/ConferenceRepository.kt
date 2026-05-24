package com.droidcon.global.data.repository

import com.droidcon.global.data.local.ConferenceDatabase
import com.droidcon.global.data.local.entity.SessionEntity
import com.droidcon.global.data.local.entity.SpeakerEntity
import com.droidcon.global.data.remote.ConferenceApi
import com.droidcon.global.data.remote.model.SessionDto
import com.droidcon.global.data.remote.model.SpeakerDto
import com.droidcon.global.domain.model.Session
import com.droidcon.global.domain.model.Speaker
import com.droidcon.global.domain.model.SpeakerLoadState
import com.droidcon.global.domain.model.SpeakerSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConferenceRepository(
    private val api: ConferenceApi,
    private val database: ConferenceDatabase,
) {
    private val refreshMutex = Mutex()
    private val speakerSyncState = MutableStateFlow(SpeakerSyncState())

    fun observeSessions(): Flow<List<Session>> =
        database.sessionDao().observeAll().map { sessions -> sessions.map { it.toDomain() } }

    fun observeSpeakers(): Flow<List<Speaker>> =
        database.speakerDao().observeAll().map { speakers -> speakers.map { it.toDomain() } }

    fun observeSpeakerSyncState(): Flow<SpeakerSyncState> = speakerSyncState.asStateFlow()

    suspend fun refresh() {
        refreshMutex.withLock {
            try {
                val sessions = api.getSessions()
                database.conferenceSyncDao().replaceSessions(sessions.map { it.toEntity() })

                speakerSyncState.value = SpeakerSyncState(
                    loadStateBySessionId = sessions.associate { it.id to SpeakerLoadState.Pending },
                    refreshInProgress = true,
                )

                for (session in sessions) {
                    updateSpeakerLoadState(session.id, SpeakerLoadState.Loading)
                    try {
                        delay(SPEAKER_REQUEST_INTERVAL_MS)
                        val speakers = api.getSpeakers(session.id)
                        val speakerEntities = speakers.map { speaker ->
                            speaker.copy(
                                sessionId = speaker.sessionId.ifBlank { session.id },
                            ).toEntity()
                        }
                        database.conferenceSyncDao().replaceSpeakersForSession(
                            sessionId = session.id,
                            speakers = speakerEntities,
                        )
                        updateSpeakerLoadState(session.id, SpeakerLoadState.Loaded)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        updateSpeakerLoadState(session.id, SpeakerLoadState.Failed)
                    }
                }
            } finally {
                speakerSyncState.value = speakerSyncState.value.copy(refreshInProgress = false)
            }
        }
    }

    private fun updateSpeakerLoadState(sessionId: String, state: SpeakerLoadState) {
        speakerSyncState.value = speakerSyncState.value.copy(
            loadStateBySessionId = speakerSyncState.value.loadStateBySessionId + (sessionId to state),
        )
    }

    private companion object {
        /** Spacing between speaker requests to stay under mock API rate limits. */
        const val SPEAKER_REQUEST_INTERVAL_MS = 450L
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
        isServiceSession = isServiceSession,
    )

private fun SpeakerDto.toEntity(): SpeakerEntity =
    SpeakerEntity(
        id = id,
        name = name,
        bio = bio,
        avatar = avatar,
        company = company,
        sessionId = sessionId,
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
        isServiceSession = isServiceSession,
    )

private fun SpeakerEntity.toDomain(): Speaker =
    Speaker(
        id = id,
        name = name,
        bio = bio,
        avatar = avatar,
        company = company,
        sessionId = sessionId,
    )
