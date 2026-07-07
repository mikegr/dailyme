package com.dailyme.app

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun createNetworkMonitor(): NetworkMonitor = object : NetworkMonitor {
    private val _isOnline = MutableStateFlow(window.navigator.onLine)
    override val isOnline: StateFlow<Boolean> = _isOnline

    init {
        window.addEventListener("online", { _isOnline.value = true })
        window.addEventListener("offline", { _isOnline.value = false })
    }
}
