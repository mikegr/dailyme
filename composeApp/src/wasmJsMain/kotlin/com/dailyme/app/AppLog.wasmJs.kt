package com.dailyme.app

private fun consoleError(message: String): Unit = js("console.error(message)")

actual object AppLog {
    actual fun e(message: String, throwable: Throwable?) {
        val details = throwable?.let { ": ${it.message}\n${it.stackTraceToString()}" } ?: ""
        consoleError("[DailyMe] $message$details")
    }
}
