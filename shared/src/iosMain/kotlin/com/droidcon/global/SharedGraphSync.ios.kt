package com.droidcon.global

import platform.Foundation.NSLock

private val lock = NSLock()

internal actual fun <R> sharedGraphSynchronized(block: () -> R): R {
    lock.lock()
    try {
        return block()
    } finally {
        lock.unlock()
    }
}
