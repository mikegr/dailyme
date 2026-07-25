package com.dailyme.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ListingResult(val items: List<GitHubContentItem>)

data class LoadedFileResult(val content: String, val hasPendingChange: Boolean)

sealed class SaveOutcome {
    data class Saved(val sha: String) : SaveOutcome()
    data object Queued : SaveOutcome()
}

data class WikiLinkResolution(val path: String, val created: Boolean)

class RepositoryFileNotFoundException(path: String) : Exception("File not found: $path")

private val journalNamePattern = Regex("""^\d{4}_\d{2}_\d{2}$""")

private const val POLL_INTERVAL_MILLIS = 15_000L

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class RepositoryClient(
    private val localGit: LocalGitRepository,
    private val callLog: CallLog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pushLock = Mutex()
    private var repoLabel: String = ""
    private var isRepositoryReady = false

    private val _unpushedCommits = MutableStateFlow<List<CommitInfo>>(emptyList())
    val unpushedCommits: StateFlow<List<CommitInfo>> = _unpushedCommits
    val callLogEntries: StateFlow<List<CallLogEntry>> = callLog.entries

    init {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MILLIS)
                retryPush()
            }
        }
    }

    /**
     * Clones (or opens the existing) local working copy for `$owner/$repo@$branch` and pulls
     * the latest from origin. A clone failure (bad token, no network, repo doesn't exist)
     * propagates to the caller; a pull failure is logged and swallowed — the existing local
     * clone is still usable offline.
     */
    suspend fun ensureRepositoryReady(owner: String, repo: String, branch: String) {
        repoLabel = "$owner/$repo"
        localGit.ensureCloned(owner, repo, branch)
        isRepositoryReady = true
        try {
            when (val result = localGit.pull()) {
                is PullOutcome.ConflictsNeedResolution -> AppLog.e("Pull couldn't merge for $repoLabel@$branch: ${result.message}")
                is PullOutcome.Failed -> AppLog.e("Pull failed for $repoLabel@$branch: ${result.message}")
                PullOutcome.UpToDate, PullOutcome.FastForwarded, PullOutcome.Merged -> Unit
            }
        } catch (e: Exception) {
            AppLog.e("Pull failed for $repoLabel@$branch — using existing local clone", e)
        }
        refreshUnpushedCommits()
    }

    private suspend fun refreshUnpushedCommits() {
        _unpushedCommits.value = try {
            localGit.unpushedCommits()
        } catch (e: Exception) {
            AppLog.e("Failed to list unpushed commits", e)
            emptyList()
        }
    }

    suspend fun listContents(owner: String, repo: String, branch: String, path: String): ListingResult =
        ListingResult(localGit.listDirectory(path))

    suspend fun getFile(owner: String, repo: String, branch: String, path: String): LoadedFileResult {
        val content = localGit.readFile(path) ?: throw RepositoryFileNotFoundException(path)
        return LoadedFileResult(content, hasPendingChange = _unpushedCommits.value.isNotEmpty())
    }

    /**
     * Resolves a `#tag` or `[[wiki link]]` reference to a file path, checking the `journals`
     * folder first and then `pages`. Returns null if no matching file exists in either.
     */
    suspend fun resolveWikiLink(owner: String, repo: String, branch: String, name: String): String? {
        for (folder in listOf("journals", "pages")) {
            val path = "$folder/$name.md"
            if (localGit.readFile(path) != null) return path
        }
        return null
    }

    /**
     * Resolves a `#tag`/`[[wiki link]]` name to a file path, creating an empty file if it
     * doesn't exist yet. Names matching the journal date pattern (`yyyy_MM_dd`, same as
     * `todayJournalFileName()`) are created under `journals/`; everything else under `pages/`.
     */
    suspend fun resolveOrCreateWikiLink(
        owner: String,
        repo: String,
        branch: String,
        name: String,
        commitMessage: String,
    ): WikiLinkResolution {
        resolveWikiLink(owner, repo, branch, name)?.let { return WikiLinkResolution(it, created = false) }
        val folder = if (journalNamePattern.matches(name)) "journals" else "pages"
        val path = "$folder/$name.md"
        saveFile(owner, repo, branch, path, newContent = "", commitMessage = commitMessage)
        return WikiLinkResolution(path, created = true)
    }

    /**
     * All page names available for `#tag`/`[[wiki link]]` autocompletion: every `.md` file
     * (extension stripped) under `journals/` and `pages/`, deduplicated. Either folder missing
     * is treated as contributing no names.
     */
    suspend fun listPageNames(owner: String, repo: String, branch: String): List<String> {
        val names = mutableSetOf<String>()
        for (folder in listOf("journals", "pages")) {
            try {
                listContents(owner, repo, branch, folder).items
                    .filter { it.type == "file" && it.name.endsWith(".md", ignoreCase = true) }
                    .mapTo(names) { it.name.removeSuffix(".md").removeSuffix(".MD") }
            } catch (e: Exception) {
                // Folder doesn't exist — no pages from it.
            }
        }
        return names.sortedBy { it.lowercase() }
    }

    /**
     * Paths of every page (under `journals/` or `pages/`, excluding [excludePath] itself) whose
     * content links back to [targetName] via `#tag` or `[[wiki link]]`. Reads straight from the
     * local working copy.
     */
    suspend fun findBacklinks(
        owner: String,
        repo: String,
        branch: String,
        targetName: String,
        excludePath: String,
    ): List<String> {
        val results = mutableListOf<String>()
        for (folder in listOf("journals", "pages")) {
            val items = try {
                listContents(owner, repo, branch, folder).items
            } catch (e: Exception) {
                continue
            }
            for (item in items) {
                if (item.type != "file" || !item.name.endsWith(".md", ignoreCase = true)) continue
                if (item.path == excludePath) continue
                val content = try {
                    getFile(owner, repo, branch, item.path).content
                } catch (e: Exception) {
                    AppLog.e("Failed to load \"${item.path}\" while scanning for backlinks", e)
                    continue
                }
                if (targetName in extractWikiLinkNames(content)) {
                    results.add(item.path)
                }
            }
        }
        return results.sortedBy { it.substringAfterLast('/').lowercase() }
    }

    /**
     * Writes and commits [newContent] to [path] locally (always succeeds instantly, even
     * offline), then makes one immediate push attempt so the UI can report success right away
     * when online. If the push fails, the commit stays local and is retried automatically in
     * the background (see [retryPush]).
     */
    suspend fun saveFile(
        owner: String,
        repo: String,
        branch: String,
        path: String,
        newContent: String,
        commitMessage: String,
    ): SaveOutcome {
        val commit = localGit.writeAndCommit(path, newContent, commitMessage)
        refreshUnpushedCommits()
        val outcome = attemptPushNow()
        return if (outcome is PushOutcome.Pushed) SaveOutcome.Saved(commit.sha) else SaveOutcome.Queued
    }

    /** Discards every not-yet-pushed local commit, resetting to match the remote branch. */
    suspend fun discardAllUnpushedChanges() {
        localGit.discardAllUnpushed()
        refreshUnpushedCommits()
    }

    suspend fun listCallLog(): List<CallLogEntry> = callLog.all()

    /** Attempts a push if one isn't already in progress; skips (rather than waiting) otherwise. Safe to call concurrently. */
    suspend fun retryPush() {
        if (!isRepositoryReady) return
        if (!pushLock.tryLock()) return
        try {
            doPush()
        } finally {
            pushLock.unlock()
        }
    }

    /** Attempts a push, waiting for any in-progress attempt first, so this and [retryPush] never race. */
    private suspend fun attemptPushNow(): PushOutcome {
        if (!isRepositoryReady) return PushOutcome.NothingToPush
        return pushLock.withLock { doPush() }
    }

    /**
     * Pulls (fetching and auto-merging any remote-only commits) before pushing, so a push
     * that would otherwise be rejected as non-fast-forward (the remote moved on since our
     * last pull) instead merges locally first. A real merge conflict is reported as a push
     * failure rather than attempted automatically.
     */
    private suspend fun doPush(): PushOutcome {
        try {
            when (val pullResult = localGit.pull()) {
                is PullOutcome.ConflictsNeedResolution -> {
                    val message = "Couldn't merge remote changes automatically: ${pullResult.message}"
                    AppLog.e(message)
                    callLog.record(CallLogEntry(nowMillis(), repoLabel, CallOutcome.FAILURE, message))
                    return PushOutcome.Failed(message)
                }
                is PullOutcome.Failed ->
                    AppLog.e("Pull before push failed for $repoLabel: ${pullResult.message}")
                PullOutcome.UpToDate, PullOutcome.FastForwarded, PullOutcome.Merged -> Unit
            }
        } catch (e: Exception) {
            AppLog.e("Pull before push failed for $repoLabel", e)
        }

        val outcome = try {
            localGit.push()
        } catch (e: Exception) {
            AppLog.e("Push failed for $repoLabel", e)
            PushOutcome.Failed(e.message ?: "Push failed")
        }
        refreshUnpushedCommits()
        when (outcome) {
            PushOutcome.NothingToPush -> Unit
            PushOutcome.Pushed -> callLog.record(CallLogEntry(nowMillis(), repoLabel, CallOutcome.SUCCESS, "Pushed"))
            is PushOutcome.Rejected ->
                callLog.record(CallLogEntry(nowMillis(), repoLabel, CallOutcome.FAILURE, outcome.message))
            is PushOutcome.Failed ->
                callLog.record(CallLogEntry(nowMillis(), repoLabel, CallOutcome.FAILURE, outcome.message))
        }
        return outcome
    }
}
