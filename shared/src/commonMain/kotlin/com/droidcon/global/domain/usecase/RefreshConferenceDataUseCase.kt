package com.droidcon.global.domain.usecase

import com.droidcon.global.data.repository.ConferenceRepository

class RefreshConferenceDataUseCase(
    private val repository: ConferenceRepository
) {
    suspend operator fun invoke() = repository.refresh()
}
