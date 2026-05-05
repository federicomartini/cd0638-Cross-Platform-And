package com.droidcon.global.domain.usecase

import com.droidcon.global.data.repository.ConferenceRepository
import com.droidcon.global.domain.model.Speaker
import kotlinx.coroutines.flow.Flow

class GetSpeakersUseCase(
    private val repository: ConferenceRepository
) {
    operator fun invoke(): Flow<List<Speaker>> = repository.observeSpeakers()
}
