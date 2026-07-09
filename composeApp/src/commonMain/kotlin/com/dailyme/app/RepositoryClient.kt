package com.dailyme.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ListingResult(
    val items: List<GitHubContentItem>,
    val isFromCache: Boolean,
)

data class LoadedFileResult(
    val sha: String?,
    val content: String,
    val isFromCache: Boolean,
    val hasPendingChange: Boolean,
)

sealed class SaveOutcome {
    data class Saved(val sha: String) : SaveOutcome()
    data object Queued : SaveOutcome()
}

private const val BASE_BACKOFF_MILLIS = 5_000L
private const val MAX_BACKOFF_MILLIS = 10 * 60_000L
private const val POLL_INTERVAL_MILLIS = 15_000L

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun backoffMillis(attempts: Int): Long {
    if (attempts <= 0) return 0L
    val shift = (attempts - 1).coerceAtMost(10)
    return (BASE_BACKOFF_MILLIS shl shift).coerceAtMost(MAX_BACKOFF_MILLIS)
}

class RepositoryClient(
    private val api: GitHubApi,
    private val cache: OfflineCache,
    private val pendingChanges: PendingChangeQueue,
    private val callLog: CallLog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processingLock = Mutex()

    val pendingChangeList: StateFlow<List<PendingChange>> = pendingChanges.changes
    val callLogEntries: StateFlow<List<CallLogEntry>> = callLog.entries

    init {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MILLIS)
                processDueChanges()
            }
        }
    }

    suspend fun refreshPendingChanges() {
        pendingChanges.all()
    }

    /**
     * Checks the branch's latest commit on GitHub against the last-known one, clearing the
     * entire offline cache if it moved on (so stale listings/files aren't served indefinitely).
     * Silently does nothing if the check fails (e.g. offline) — the existing cache is kept.
     */
    suspend fun refreshCacheValidity(owner: String, repo: String, branch: String) {
        try {
            val latestSha = api.getLatestCommitSha(owner, repo, branch)
            val lastKnownSha = cache.getLastKnownCommitSha(owner, repo, branch)
            if (lastKnownSha != null && lastKnownSha != latestSha) {
                cache.clear()
            }
            cache.setLastKnownCommitSha(owner, repo, branch, latestSha)
        } catch (e: Exception) {
            // Offline or API error — keep using whatever is already cached.
        }
    }

    suspend fun listContents(owner: String, repo: String, branch: String, path: String): ListingResult {
        cache.getListing(owner, repo, branch, path)?.let { return ListingResult(it, isFromCache = true) }

        val fresh = api.listContents(owner, repo, path, branch)
        cache.putListing(owner, repo, branch, path, fresh)
        return ListingResult(fresh, isFromCache = false)
    }

    suspend fun getFile(owner: String, repo: String, branch: String, path: String): LoadedFileResult {
        pendingChanges.find(owner, repo, branch, path)?.let { pending ->
            return LoadedFileResult(
                sha = pending.baseSha,
                content = pending.newContent,
                isFromCache = false,
                hasPendingChange = true,
            )
        }

        cache.getFile(owner, repo, branch, path)?.let { cached ->
            return LoadedFileResult(
                sha = cached.sha,
                content = cached.content?.let { decodeBase64Content(it) } ?: "",
                isFromCache = true,
                hasPendingChange = false,
            )
        }

        val fresh = api.getFile(owner, repo, path, branch)
        cache.putFile(owner, repo, branch, path, fresh)
        return LoadedFileResult(
            sha = fresh.sha,
            content = fresh.content?.let { decodeBase64Content(it) } ?: "",
            isFromCache = false,
            hasPendingChange = false,
        )
    }

    /**
     * Resolves a `#tag` or `[[wiki link]]` reference to a file path, checking the `journals`
     * folder first and then `pages`. Returns null if no matching file exists in either.
     */
    suspend fun resolveWikiLink(owner: String, repo: String, branch: String, name: String): String? {
        for (folder in listOf("journals", "pages")) {
            val path = "$folder/$name.md"
            val found = try {
                getFile(owner, repo, branch, path)
                true
            } catch (e: Exception) {
                false
            }
            if (found) return path
        }
        return null
    }

    /**
     * Queues the commit durably first, then makes one immediate attempt so the UI can report
     * success right away when online. If that attempt fails, the change stays queued and is
     * retried automatically in the background with exponential backoff (see [processDueChanges]).
     */
    suspend fun saveFile(
        owner: String,
        repo: String,
        branch: String,
        path: String,
        newContent: String,
        baseSha: String?,
        commitMessage: String,
    ): SaveOutcome {
        val change = PendingChange(
            owner = owner,
            repo = repo,
            branch = branch,
            path = path,
            newContent = newContent,
            baseSha = baseSha,
            commitMessage = commitMessage,
        )
        pendingChanges.upsert(change)
        val sha = attemptChange(change)
        return if (sha != null) SaveOutcome.Saved(sha) else SaveOutcome.Queued
    }

    suspend fun listPendingChanges(): List<PendingChange> = pendingChanges.all()

    suspend fun discardPendingChange(change: PendingChange) {
        pendingChanges.remove(change.owner, change.repo, change.branch, change.path)
    }

    suspend fun listCallLog(): List<CallLogEntry> = callLog.all()

    /** Attempts every queued change whose backoff has elapsed. Safe to call concurrently. */
    suspend fun processDueChanges() {
        if (!processingLock.tryLock()) return
        try {
            val now = nowMillis()
            for (change in pendingChanges.all()) {
                if (change.nextAttemptAtMillis > now) continue
                // Re-fetch in case it was discarded or updated concurrently since the loop started.
                val current = pendingChanges.find(change.owner, change.repo, change.branch, change.path) ?: continue
                attemptChange(current)
            }
        } finally {
            processingLock.unlock()
        }
    }

    /** Makes one attempt at [change]. Returns the new sha on success, or null if it's still queued. */
    private suspend fun attemptChange(change: PendingChange): String? {
        return try {
            api.updateFile(
                change.owner,
                change.repo,
                change.path,
                change.branch,
                change.commitMessage,
                change.newContent,
                change.baseSha,
            )
            val refreshed = api.getFile(change.owner, change.repo, change.path, change.branch)
            cache.putFile(change.owner, change.repo, change.branch, change.path, refreshed)
            // The parent folder's cached listing may now be missing (or stale for) this file.
            cache.removeListing(change.owner, change.repo, change.branch, change.path.substringBeforeLast('/', ""))
            pendingChanges.remove(change.owner, change.repo, change.branch, change.path)
            callLog.record(
                CallLogEntry(
                    timestampMillis = nowMillis(),
                    path = change.path,
                    outcome = CallOutcome.SUCCESS,
                    message = "Saved (${refreshed.sha.take(7)})",
                )
            )
            refreshed.sha
        } catch (e: Exception) {
            val message = if (e is GitHubApiException) {
                "${e.message} (HTTP ${e.statusCode})"
            } else {
                e.message ?: "Network error"
            }
            callLog.record(
                CallLogEntry(
                    timestampMillis = nowMillis(),
                    path = change.path,
                    outcome = CallOutcome.FAILURE,
                    message = message,
                )
            )
            val attempts = change.attempts + 1
            pendingChanges.upsert(
                change.copy(
                    attempts = attempts,
                    nextAttemptAtMillis = nowMillis() + backoffMillis(attempts),
                    lastError = message,
                )
            )
            null
        }
    }
}
