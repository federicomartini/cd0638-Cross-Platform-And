package com.droidcon.global

/**
 * Serializes SharedGraph initialization across platforms.
 * JVM-only primitives (@Volatile, synchronized) cannot be used from commonMain.
 */
internal expect fun <R> sharedGraphSynchronized(block: () -> R): R
