package com.dailyme.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val pendingJson = Json { ignoreUnknownKeys = true }
private const val PENDING_CHANGES_KEY = "pending_changes"

@Serializable
data class PendingChange(
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
    val newContent: String,
    val baseSha: String?,
    val commitMessage: String,
    val attempts: Int = 0,
    val nextAttemptAtMillis: Long = 0L,
    val lastError: String? = null,
)

class PendingChangeQueue(private val store: KeyValueStore) {
    private val _changes = MutableStateFlow<List<PendingChange>>(emptyList())
    val changes: StateFlow<List<PendingChange>> = _changes
    private var loaded = false

    // saveFile() can attempt a change directly while the background poller processes others
    // concurrently; this guards the read-modify-write below from racing and losing an update.
    private val mutex = Mutex()

    private suspend fun ensureLoaded() {
        if (loaded) return
        val raw = store.get(PENDING_CHANGES_KEY)
        _changes.value = raw
            ?.let { runCatching { pendingJson.decodeFromString<List<PendingChange>>(it) }.getOrNull() }
            ?: emptyList()
        loaded = true
    }

    private fun sameTarget(a: PendingChange, owner: String, repo: String, branch: String, path: String) =
        a.owner == owner && a.repo == repo && a.branch == branch && a.path == path

    suspend fun all(): List<PendingChange> = mutex.withLock {
        ensureLoaded()
        _changes.value
    }

    suspend fun find(owner: String, repo: String, branch: String, path: String): PendingChange? = mutex.withLock {
        ensureLoaded()
        _changes.value.find { sameTarget(it, owner, repo, branch, path) }
    }

    suspend fun upsert(change: PendingChange) = mutex.withLock {
        ensureLoaded()
        val current = _changes.value.toMutableList()
        val index = current.indexOfFirst { sameTarget(it, change.owner, change.repo, change.branch, change.path) }
        if (index >= 0) current[index] = change else current.add(change)
        persist(current)
    }

    suspend fun remove(owner: String, repo: String, branch: String, path: String) = mutex.withLock {
        ensureLoaded()
        val current = _changes.value.filterNot { sameTarget(it, owner, repo, branch, path) }
        persist(current)
    }

    private suspend fun persist(changes: List<PendingChange>) {
        _changes.value = changes
        store.put(PENDING_CHANGES_KEY, pendingJson.encodeToString(changes))
    }
}
