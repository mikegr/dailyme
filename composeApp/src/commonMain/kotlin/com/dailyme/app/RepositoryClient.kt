package com.dailyme.app

import kotlinx.coroutines.flow.StateFlow

data class ListingResult(
    val items: List<GitHubContentItem>,
    val isFromCache: Boolean,
)

data class LoadedFileResult(
    val sha: String,
    val content: String,
    val isFromCache: Boolean,
    val hasPendingChange: Boolean,
)

sealed class SaveOutcome {
    data class Saved(val sha: String) : SaveOutcome()
    data object Queued : SaveOutcome()
}

data class SyncOutcome(val succeeded: Int, val conflicted: Int, val remaining: Int)

class RepositoryClient(
    private val api: GitHubApi,
    private val cache: OfflineCache,
    private val pendingChanges: PendingChangeQueue,
) {
    val pendingChangeList: StateFlow<List<PendingChange>> = pendingChanges.changes

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

    suspend fun saveFile(
        owner: String,
        repo: String,
        branch: String,
        path: String,
        newContent: String,
        baseSha: String,
        commitMessage: String,
    ): SaveOutcome {
        return try {
            api.updateFile(owner, repo, path, branch, commitMessage, newContent, baseSha)
            val refreshed = api.getFile(owner, repo, path, branch)
            cache.putFile(owner, repo, branch, path, refreshed)
            pendingChanges.remove(owner, repo, branch, path)
            SaveOutcome.Saved(refreshed.sha)
        } catch (e: GitHubApiException) {
            throw e
        } catch (e: Exception) {
            pendingChanges.upsert(
                PendingChange(
                    owner = owner,
                    repo = repo,
                    branch = branch,
                    path = path,
                    newContent = newContent,
                    baseSha = baseSha,
                    commitMessage = commitMessage,
                )
            )
            SaveOutcome.Queued
        }
    }

    suspend fun listPendingChanges(): List<PendingChange> = pendingChanges.all()

    suspend fun discardPendingChange(change: PendingChange) {
        pendingChanges.remove(change.owner, change.repo, change.branch, change.path)
    }

    suspend fun syncPendingChanges(): SyncOutcome {
        val queue = pendingChanges.all()
        var succeeded = 0
        var conflicted = 0
        for (change in queue) {
            try {
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
                pendingChanges.remove(change.owner, change.repo, change.branch, change.path)
                succeeded++
            } catch (e: GitHubApiException) {
                if (e.statusCode == 409 || e.statusCode == 422) conflicted++
                // left in the queue either way; the user can discard it from the pending-changes screen
            } catch (e: Exception) {
                break // likely lost connectivity again; stop this round
            }
        }
        return SyncOutcome(succeeded, conflicted, pendingChanges.all().size)
    }
}
