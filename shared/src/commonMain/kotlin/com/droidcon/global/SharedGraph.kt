package com.droidcon.global

import com.droidcon.global.data.local.ConferenceDatabase
import com.droidcon.global.data.remote.ConferenceApi
import com.droidcon.global.data.repository.ConferenceRepository
import com.droidcon.global.domain.usecase.GetSpeakersUseCase
import com.droidcon.global.domain.usecase.GetSessionsUseCase
import com.droidcon.global.domain.usecase.RefreshConferenceDataUseCase

object SharedGraph {
    @Volatile
    private var initialized = false
    private val lock = Any()
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
    val refreshConferenceDataUseCase: RefreshConferenceDataUseCase by lazy {
        RefreshConferenceDataUseCase(repository)
    }

    fun init(database: ConferenceDatabase) {
        synchronized(lock) {
            if (!initialized) {
                this.database = database
                initialized = true
            }
        }
    }

    fun initIfNeeded(databaseProvider: () -> ConferenceDatabase) {
        if (initialized) return
        synchronized(lock) {
            if (!initialized) {
                this.database = databaseProvider()
                initialized = true
            }
        }
    }
}
