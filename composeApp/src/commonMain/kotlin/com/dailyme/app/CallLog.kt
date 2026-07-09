package com.dailyme.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val callLogJson = Json { ignoreUnknownKeys = true }
private const val CALL_LOG_KEY = "call_log"
private const val CALL_LOG_MAX_ENTRIES = 50

enum class CallOutcome {
    SUCCESS,
    FAILURE,
}

@Serializable
data class CallLogEntry(
    val timestampMillis: Long,
    val path: String,
    val outcome: CallOutcome,
    val message: String,
)

/** A bounded, persisted history of commit-call attempts, most recent first. */
class CallLog(private val store: KeyValueStore) {
    private val _entries = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val entries: StateFlow<List<CallLogEntry>> = _entries
    private var loaded = false

    // record() can be called concurrently from saveFile()'s direct attempt and the background
    // poller processing other changes; this guards the read-modify-write from racing.
    private val mutex = Mutex()

    private suspend fun ensureLoaded() {
        if (loaded) return
        val raw = store.get(CALL_LOG_KEY)
        _entries.value = raw
            ?.let { runCatching { callLogJson.decodeFromString<List<CallLogEntry>>(it) }.getOrNull() }
            ?: emptyList()
        loaded = true
    }

    suspend fun all(): List<CallLogEntry> = mutex.withLock {
        ensureLoaded()
        _entries.value
    }

    suspend fun record(entry: CallLogEntry) = mutex.withLock {
        ensureLoaded()
        val updated = (listOf(entry) + _entries.value).take(CALL_LOG_MAX_ENTRIES)
        _entries.value = updated
        store.put(CALL_LOG_KEY, callLogJson.encodeToString(updated))
    }
}
