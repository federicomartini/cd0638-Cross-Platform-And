package com.droidcon.global.data.local

import android.content.Context
import androidx.room.Room

fun createAndroidDatabase(context: Context): ConferenceDatabase =
    Room.databaseBuilder<ConferenceDatabase>(
        context = context.applicationContext,
        name = "conference.db"
    )
        .build()
