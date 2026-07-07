package com.dailyme.app

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

expect fun createNetworkMonitor(): NetworkMonitor
