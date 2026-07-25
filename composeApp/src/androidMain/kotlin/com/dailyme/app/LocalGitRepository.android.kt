package com.dailyme.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

private val COMMIT_IDENTITY = PersonIdent("DailyMe", "dailyme@users.noreply.github.com")

class AndroidLocalGitRepository(
    private val context: Context,
    private val tokenProvider: suspend () -> String?,
) : LocalGitRepository {
    private var git: Git? = null
    private var currentKey: String? = null

    private fun dirFor(owner: String, repo: String, branch: String): File =
        File(context.filesDir, "repo-$owner-$repo-$branch")

    private suspend fun credentialsProvider(): UsernamePasswordCredentialsProvider {
        val token = tokenProvider() ?: ""
        return UsernamePasswordCredentialsProvider(token, "")
    }

    override suspend fun ensureCloned(owner: String, repo: String, branch: String) = withContext(Dispatchers.IO) {
        val key = "$owner/$repo@$branch"
        if (currentKey == key && git != null) return@withContext
        git?.close()
        val dir = dirFor(owner, repo, branch)
        git = if (File(dir, ".git").isDirectory) {
            Git.open(dir)
        } else {
            dir.mkdirs()
            Git.cloneRepository()
                .setURI("https://github.com/$owner/$repo.git")
                .setDirectory(dir)
                .setBranch(branch)
                .setCredentialsProvider(credentialsProvider())
                .call()
        }
        // So JGit-internal merge commits (created during pull, see below) have a valid
        // identity too — writeAndCommit() sets it explicitly per-commit, but the merge
        // commits pull() can create go through JGit's own default PersonIdent otherwise.
        val config = git!!.repository.config
        config.setString("user", null, "name", COMMIT_IDENTITY.name)
        config.setString("user", null, "email", COMMIT_IDENTITY.emailAddress)
        config.save()
        currentKey = key
    }

    override suspend fun pull(): PullOutcome = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext PullOutcome.Failed("Repository not cloned yet")
        try {
            // No setFastForward restriction: fast-forward when possible, otherwise merge
            // automatically (a real merge commit) so a push that would otherwise be
            // rejected as non-fast-forward can proceed. Only a genuine content conflict
            // (same lines touched on both sides) fails this and needs manual resolution.
            val result = g.pull()
                .setCredentialsProvider(credentialsProvider())
                .call()
            val mergeStatus = result.mergeResult?.mergeStatus
            when {
                !result.isSuccessful -> PullOutcome.ConflictsNeedResolution(
                    mergeStatus?.toString() ?: "Remote has diverged from the local clone"
                )
                mergeStatus == MergeResult.MergeStatus.ALREADY_UP_TO_DATE -> PullOutcome.UpToDate
                mergeStatus == MergeResult.MergeStatus.FAST_FORWARD ||
                    mergeStatus == MergeResult.MergeStatus.FAST_FORWARD_SQUASHED -> PullOutcome.FastForwarded
                else -> PullOutcome.Merged
            }
        } catch (e: Exception) {
            AppLog.e("git pull failed", e)
            PullOutcome.Failed(e.message ?: "Pull failed")
        }
    }

    override suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext null
        val file = File(g.repository.workTree, path)
        if (!file.isFile) null else file.readText()
    }

    override suspend fun listDirectory(path: String): List<GitHubContentItem> = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext emptyList()
        val dir = if (path.isEmpty()) g.repository.workTree else File(g.repository.workTree, path)
        val children = dir.listFiles() ?: return@withContext emptyList()
        children
            .filterNot { it.name == ".git" }
            .map { child ->
                val relativePath = if (path.isEmpty()) child.name else "$path/${child.name}"
                GitHubContentItem(
                    name = child.name,
                    path = relativePath,
                    sha = contentFingerprint(child),
                    type = if (child.isDirectory) "dir" else "file",
                    size = if (child.isFile) child.length() else 0L,
                )
            }
    }

    override suspend fun writeAndCommit(path: String, content: String, message: String): CommitInfo =
        withContext(Dispatchers.IO) {
            val g = git ?: error("Repository not cloned yet")
            val file = File(g.repository.workTree, path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            g.add().addFilepattern(path).call()
            val commit = g.commit()
                .setMessage(message)
                .setAuthor(COMMIT_IDENTITY)
                .setCommitter(COMMIT_IDENTITY)
                .call()
            CommitInfo(commit.name, message, commit.commitTime * 1000L)
        }

    override suspend fun push(): PushOutcome = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext PushOutcome.Failed("Repository not cloned yet")
        try {
            if (unpushedCommitsSync(g).isEmpty()) return@withContext PushOutcome.NothingToPush
            val results = g.push().setCredentialsProvider(credentialsProvider()).call()
            val rejected = results
                .flatMap { it.remoteUpdates }
                .firstOrNull { it.status != RemoteRefUpdate.Status.OK && it.status != RemoteRefUpdate.Status.UP_TO_DATE }
            if (rejected != null) {
                PushOutcome.Rejected(rejected.message ?: rejected.status.toString())
            } else {
                PushOutcome.Pushed
            }
        } catch (e: Exception) {
            AppLog.e("git push failed", e)
            PushOutcome.Failed(e.message ?: "Push failed")
        }
    }

    override suspend fun unpushedCommits(): List<CommitInfo> = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext emptyList()
        unpushedCommitsSync(g)
    }

    override suspend fun discardAllUnpushed() = withContext(Dispatchers.IO) {
        val g = git ?: return@withContext
        val remoteBranchRef = "refs/remotes/origin/${g.repository.branch}"
        if (g.repository.resolve(remoteBranchRef) == null) return@withContext
        g.reset().setMode(ResetCommand.ResetType.HARD).setRef(remoteBranchRef).call()
        Unit
    }

    /** Commits reachable from HEAD but not yet from the remote-tracking branch. */
    private fun unpushedCommitsSync(g: Git): List<CommitInfo> {
        val repository = g.repository
        val head = repository.resolve("HEAD") ?: return emptyList()
        val remoteRef = repository.resolve("refs/remotes/origin/${repository.branch}")
        return commitsBetween(repository, remoteRef, head)
    }

    private fun commitsBetween(repository: Repository, since: ObjectId?, until: ObjectId): List<CommitInfo> {
        RevWalk(repository).use { walk ->
            val untilCommit = walk.parseCommit(until)
            if (since != null) walk.markUninteresting(walk.parseCommit(since))
            walk.markStart(untilCommit)
            return walk.map { CommitInfo(it.name, it.shortMessage, it.commitTime * 1000L) }
        }
    }

    /** Not a real git blob sha — just a cheap fingerprint; nothing currently depends on this being a true git object id. */
    private fun contentFingerprint(file: File): String =
        if (file.isFile) "${file.length()}-${file.lastModified()}" else "dir-${file.lastModified()}"
}

actual fun createLocalGitRepository(tokenProvider: suspend () -> String?): LocalGitRepository =
    AndroidLocalGitRepository(AndroidContextHolder.appContext, tokenProvider)
