package com.droidcon.global.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val speakerId: String = "",
    val isServiceSession: Boolean = false
)

@Serializable
data class SpeakerDto(
    val id: String,
    val name: String,
    val bio: String = "",
    val avatar: String = "",
    val company: String = "",
    val sessionId: String = ""
)
