package com.droidcon.global.domain.usecase

import com.droidcon.global.data.repository.ConferenceRepository
import com.droidcon.global.domain.model.Session
import kotlinx.coroutines.flow.Flow

class GetSessionsUseCase(
    private val repository: ConferenceRepository
) {
    operator fun invoke(): Flow<List<Session>> = repository.observeSessions()
}
