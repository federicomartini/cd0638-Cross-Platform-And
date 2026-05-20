package com.droidcon.global.data.remote

class ConferenceApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
