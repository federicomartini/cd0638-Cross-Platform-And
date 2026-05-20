package com.droidcon.global

private val lock = Any()

internal actual fun <R> sharedGraphSynchronized(block: () -> R): R {
    synchronized(lock) {
        return block()
    }
}
