package com.droidcon.global.domain.usecase

import com.droidcon.global.data.repository.ConferenceRepository
import com.droidcon.global.domain.model.SpeakerSyncState
import kotlinx.coroutines.flow.Flow

class ObserveSpeakerSyncStateUseCase(
    private val repository: ConferenceRepository,
) {
    operator fun invoke(): Flow<SpeakerSyncState> = repository.observeSpeakerSyncState()
}
