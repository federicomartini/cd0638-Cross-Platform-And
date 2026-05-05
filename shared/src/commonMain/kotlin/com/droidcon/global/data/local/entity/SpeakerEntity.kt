package com.droidcon.global.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speakers")
data class SpeakerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bio: String,
    val avatar: String,
    val company: String,
    val sessionId: String
)
