package com.droidcon.global

import com.droidcon.global.domain.model.Speaker
import com.droidcon.global.domain.model.SpeakerLoadState

fun formatSessionSpeakersListLine(
    speakers: List<Speaker>,
    loadState: SpeakerLoadState?,
    refreshInProgress: Boolean,
): String {
    if (speakers.isNotEmpty()) {
        return speakers.joinToString(prefix = "With ") { it.name }
    }
    return when (loadState) {
        SpeakerLoadState.Pending, SpeakerLoadState.Loading -> "Loading..."
        SpeakerLoadState.Failed -> "Speaker: not available"
        SpeakerLoadState.Loaded -> "No speakers listed"
        null -> if (refreshInProgress) "Loading..." else "Speaker: not available"
    }
}

fun formatSessionSpeakersDetailMessage(
    speakers: List<Speaker>,
    loadState: SpeakerLoadState?,
    refreshInProgress: Boolean,
): String? {
    if (speakers.isNotEmpty()) return null
    return when (loadState) {
        SpeakerLoadState.Pending, SpeakerLoadState.Loading -> "Loading speakers..."
        SpeakerLoadState.Failed -> "Speaker data is not available."
        SpeakerLoadState.Loaded -> "No speakers listed for this session."
        null -> if (refreshInProgress) "Loading speakers..." else "Speaker data is not available."
    }
}
