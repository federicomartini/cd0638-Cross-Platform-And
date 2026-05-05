package com.droidcon.global.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val bio: String,
    val avatar: String,
    val company: String,
    val sessionId: String
)
