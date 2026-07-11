package com.dailyme.app

/** Cross-platform error logging: Logcat on Android, the browser console on wasmJs. */
expect object AppLog {
    fun e(message: String, throwable: Throwable? = null)
}
