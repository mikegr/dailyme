package com.dailyme.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun todayJournalFileName(): String =
    SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date())
