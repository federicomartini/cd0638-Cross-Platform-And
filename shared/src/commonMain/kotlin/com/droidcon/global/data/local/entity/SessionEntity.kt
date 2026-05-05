package com.droidcon.global.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val speakerId: String,
    val isServiceSession: Boolean
)
