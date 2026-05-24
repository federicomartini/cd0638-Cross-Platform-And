package com.droidcon.global

import com.droidcon.global.data.local.ConferenceDatabase
import com.droidcon.global.data.remote.ConferenceApi
import com.droidcon.global.data.repository.ConferenceRepository
import com.droidcon.global.domain.usecase.GetSpeakersUseCase
import com.droidcon.global.domain.usecase.GetSessionsUseCase
import com.droidcon.global.domain.usecase.ObserveSpeakerSyncStateUseCase
import com.droidcon.global.domain.usecase.RefreshConferenceDataUseCase

object SharedGraph {
    private var initialized = false
    private lateinit var database: ConferenceDatabase

    val repository: ConferenceRepository by lazy {
        check(initialized) { "SharedGraph must be initialized first." }
        ConferenceRepository(
            api = ConferenceApi(),
            database = database
        )
    }
    val getSessionsUseCase: GetSessionsUseCase by lazy { GetSessionsUseCase(repository) }
    val getSpeakersUseCase: GetSpeakersUseCase by lazy { GetSpeakersUseCase(repository) }
    val observeSpeakerSyncStateUseCase: ObserveSpeakerSyncStateUseCase by lazy {
        ObserveSpeakerSyncStateUseCase(repository)
    }
    val refreshConferenceDataUseCase: RefreshConferenceDataUseCase by lazy {
        RefreshConferenceDataUseCase(repository)
    }

    fun init(database: ConferenceDatabase) {
        sharedGraphSynchronized {
            if (!initialized) {
                this.database = database
                initialized = true
            }
        }
    }

    fun initIfNeeded(databaseProvider: () -> ConferenceDatabase) {
        sharedGraphSynchronized {
            if (!initialized) {
                this.database = databaseProvider()
                initialized = true
            }
        }
    }
}
