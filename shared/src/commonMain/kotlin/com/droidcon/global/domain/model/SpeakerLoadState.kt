package com.droidcon.global.domain.model

enum class SpeakerLoadState {
    Pending,
    Loading,
    Loaded,
    Failed,
}

data class SpeakerSyncState(
    val loadStateBySessionId: Map<String, SpeakerLoadState> = emptyMap(),
    val refreshInProgress: Boolean = false,
)
