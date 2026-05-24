package com.droidcon.global

import kotlinx.coroutines.flow.Flow

expect class NetworkMonitor {
    fun isOnline(): Boolean
    fun observeConnectivity(): Flow<Boolean>
}

internal expect fun createNetworkMonitor(): NetworkMonitor
