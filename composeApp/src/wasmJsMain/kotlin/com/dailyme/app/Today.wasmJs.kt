package com.dailyme.app

private external class JsDate : JsAny {
    fun getFullYear(): Int
    fun getMonth(): Int
    fun getDate(): Int
}

private fun newDate(): JsDate = js("new Date()")

actual fun todayJournalFileName(): String {
    val date = newDate()
    val month = (date.getMonth() + 1).toString().padStart(2, '0')
    val day = date.getDate().toString().padStart(2, '0')
    return "${date.getFullYear()}_${month}_${day}"
}
