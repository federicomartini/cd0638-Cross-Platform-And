package com.droidcon.global

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform