package com.dailyme.app

sealed class PushOutcome {
    data object Pushed : PushOutcome()
    data object NothingToPush : PushOutcome()
    data class Rejected(val message: String) : PushOutcome()
    data class Failed(val message: String) : PushOutcome()
}

sealed class PullOutcome {
    data object UpToDate : PullOutcome()
    data object FastForwarded : PullOutcome()
    /** Remote and local both had new commits; merged automatically with a new merge commit. */
    data object Merged : PullOutcome()
    data class ConflictsNeedResolution(val message: String) : PullOutcome()
    data class Failed(val message: String) : PullOutcome()
}

data class CommitInfo(
    val sha: String,
    val message: String,
    val timestampMillis: Long,
)

/**
 * A real local git working copy for one owner/repo/branch at a time, replacing the old
 * GitHub-Contents-API-plus-offline-cache approach: reads and writes hit the local checkout
 * directly, writes are real local commits (always succeed instantly, even offline), and
 * [push] is the only network operation — the one that can fail and gets retried.
 */
interface LocalGitRepository {
    /**
     * Clones `$owner/$repo@$branch` into a local working copy if not already present, or
     * opens the existing one if it is. Idempotent and cheap to call repeatedly.
     */
    suspend fun ensureCloned(owner: String, repo: String, branch: String)

    /** Fetches from origin and fast-forwards local history onto it. */
    suspend fun pull(): PullOutcome

    /** Null if [path] doesn't exist in the working copy. */
    suspend fun readFile(path: String): String?

    /** Immediate children of [path] (`""` for the repository root). */
    suspend fun listDirectory(path: String): List<GitHubContentItem>

    /** Writes [content] to [path] and commits it locally with [message]. Always succeeds offline. */
    suspend fun writeAndCommit(path: String, content: String, message: String): CommitInfo

    /** Pushes any local commits not yet on origin. */
    suspend fun push(): PushOutcome

    /** Local commits that haven't been pushed to origin yet, most recent first. */
    suspend fun unpushedCommits(): List<CommitInfo>

    /** Hard-resets the local branch to match the remote-tracking branch, discarding all unpushed commits. */
    suspend fun discardAllUnpushed()
}

expect fun createLocalGitRepository(tokenProvider: suspend () -> String?): LocalGitRepository
