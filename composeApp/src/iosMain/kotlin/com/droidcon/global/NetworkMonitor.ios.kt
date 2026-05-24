package com.droidcon.global

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

private var lastKnownOnline = false

@OptIn(ExperimentalForeignApi::class)
actual class NetworkMonitor {
    actual fun isOnline(): Boolean = lastKnownOnline

    actual fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val online = nw_path_get_status(path) == nw_path_status_satisfied
            lastKnownOnline = online
            trySend(online)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        trySend(lastKnownOnline)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }.distinctUntilChanged()
}

internal actual fun createNetworkMonitor(): NetworkMonitor = NetworkMonitor()
