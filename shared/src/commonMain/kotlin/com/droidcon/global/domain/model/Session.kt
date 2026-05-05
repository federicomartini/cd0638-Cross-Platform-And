package com.droidcon.global.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val speakerId: String,
    val isServiceSession: Boolean
)
