package com.dailyme.app

import android.util.Log

private const val TAG = "DailyMe"

actual object AppLog {
    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
