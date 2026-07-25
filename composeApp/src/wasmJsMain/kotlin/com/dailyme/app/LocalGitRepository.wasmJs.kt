package com.dailyme.app

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.js.Promise

private const val WORK_DIR = "/repo"

private fun decodeJsonObject(json: String): Map<String, String> = try {
    (Json.parseToJsonElement(json) as? JsonObject)
        ?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
        ?: emptyMap()
} catch (e: Exception) {
    AppLog.e("Failed to parse JSON from JS interop: $json", e)
    emptyMap()
}

private fun decodeJsonArray(json: String): List<Map<String, String>> = try {
    (Json.parseToJsonElement(json) as? JsonArray)
        ?.map { element ->
            (element as? JsonObject)?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() } ?: emptyMap()
        }
        ?: emptyList()
} catch (e: Exception) {
    AppLog.e("Failed to parse JSON array from JS interop: $json", e)
    emptyList()
}

@JsModule("isomorphic-git")
private external object Git : JsAny

@JsModule("isomorphic-git/http/web")
private external object GitHttp : JsAny

@JsModule("@isomorphic-git/lightning-fs")
private external class LightningFS(name: String) : JsAny

private fun jsClone(git: JsAny, http: JsAny, fs: JsAny, dir: String, url: String, ref: String, token: String): Promise<JsAny?> = js(
    """
    (async () => {
        try {
            await git.clone({ fs: fs, http: http, dir: dir, url: url, ref: ref, singleBranch: true, depth: 200, onAuth: function() { return { username: token }; } });
            return JSON.stringify({ ok: true });
        } catch (e) {
            return JSON.stringify({ ok: false, message: String(e && e.message || e) });
        }
    })()
    """
)

private fun jsPull(git: JsAny, http: JsAny, fs: JsAny, dir: String, ref: String, token: String): Promise<JsAny?> = js(
    """
    (async () => {
        try {
            await git.pull({ fs: fs, http: http, dir: dir, ref: ref, singleBranch: true, author: { name: 'DailyMe', email: 'dailyme@users.noreply.github.com' }, onAuth: function() { return { username: token }; } });
            return JSON.stringify({ outcome: 'ok' });
        } catch (e) {
            return JSON.stringify({ outcome: 'failed', message: String(e && e.message || e) });
        }
    })()
    """
)

private fun jsReadFile(fs: JsAny, dir: String, path: String): Promise<JsAny?> = js(
    """
    (async () => {
        try {
            const content = await fs.promises.readFile(dir + '/' + path, 'utf8');
            return JSON.stringify({ found: true, content: content });
        } catch (e) {
            return JSON.stringify({ found: false });
        }
    })()
    """
)

private fun jsListDirectory(fs: JsAny, dir: String, path: String): Promise<JsAny?> = js(
    """
    (async () => {
        const full = path ? (dir + '/' + path) : dir;
        let names;
        try {
            names = await fs.promises.readdir(full);
        } catch (e) {
            return JSON.stringify([]);
        }
        const items = [];
        for (const name of names) {
            if (name === '.git') continue;
            const childPath = path ? (path + '/' + name) : name;
            let isDir = false;
            let size = 0;
            try {
                const st = await fs.promises.stat(full + '/' + name);
                isDir = st.isDirectory();
                size = st.size || 0;
            } catch (e) {}
            items.push({ name: name, path: childPath, type: isDir ? 'dir' : 'file', size: size });
        }
        return JSON.stringify(items);
    })()
    """
)

private fun jsWriteAndCommit(git: JsAny, fs: JsAny, dir: String, path: String, content: String, message: String): Promise<JsAny?> = js(
    """
    (async () => {
        const full = dir + '/' + path;
        const parts = full.split('/');
        parts.pop();
        let acc = '';
        for (const part of parts) {
            if (!part) continue;
            acc += '/' + part;
            try { await fs.promises.mkdir(acc); } catch (e) {}
        }
        await fs.promises.writeFile(full, content, 'utf8');
        await git.add({ fs: fs, dir: dir, filepath: path });
        const sha = await git.commit({ fs: fs, dir: dir, message: message, author: { name: 'DailyMe', email: 'dailyme@users.noreply.github.com' } });
        return JSON.stringify({ sha: sha, message: message, timestampMillis: Date.now() });
    })()
    """
)

private fun jsPush(git: JsAny, http: JsAny, fs: JsAny, dir: String, token: String): Promise<JsAny?> = js(
    """
    (async () => {
        try {
            const res = await git.push({ fs: fs, http: http, dir: dir, onAuth: function() { return { username: token }; } });
            if (res.ok) return JSON.stringify({ outcome: 'pushed' });
            return JSON.stringify({ outcome: 'rejected', message: JSON.stringify(res) });
        } catch (e) {
            return JSON.stringify({ outcome: 'failed', message: String(e && e.message || e) });
        }
    })()
    """
)

private fun jsUnpushedCommits(git: JsAny, fs: JsAny, dir: String, branch: String): Promise<JsAny?> = js(
    """
    (async () => {
        let remoteOid = null;
        try {
            remoteOid = await git.resolveRef({ fs: fs, dir: dir, ref: 'refs/remotes/origin/' + branch });
        } catch (e) {}
        const commits = await git.log({ fs: fs, dir: dir, ref: 'HEAD' });
        const result = [];
        for (const c of commits) {
            if (remoteOid && c.oid === remoteOid) break;
            result.push({ sha: c.oid, message: c.commit.message.trim(), timestampMillis: c.commit.author.timestamp * 1000 });
        }
        return JSON.stringify(result);
    })()
    """
)

private fun jsDiscardAllUnpushed(git: JsAny, fs: JsAny, dir: String, branch: String): Promise<JsAny?> = js(
    """
    (async () => {
        try {
            const remoteOid = await git.resolveRef({ fs: fs, dir: dir, ref: 'refs/remotes/origin/' + branch });
            await fs.promises.writeFile(dir + '/.git/refs/heads/' + branch, remoteOid + '\n', 'utf8');
            await git.checkout({ fs: fs, dir: dir, ref: branch, force: true });
            return JSON.stringify({ ok: true });
        } catch (e) {
            return JSON.stringify({ ok: false, message: String(e && e.message || e) });
        }
    })()
    """
)

private suspend fun Promise<JsAny?>.awaitJson(): String = await()

class WasmJsLocalGitRepository(private val tokenProvider: suspend () -> String?) : LocalGitRepository {
    private var fs: JsAny? = null
    private var currentKey: String? = null
    private var currentBranch: String = ""

    override suspend fun ensureCloned(owner: String, repo: String, branch: String) {
        val key = "$owner/$repo@$branch"
        currentBranch = branch
        if (currentKey == key && fs != null) return
        val instance = LightningFS("dailyme-$key")
        val token = tokenProvider() ?: ""
        val json = jsClone(Git, GitHttp, instance, WORK_DIR, "https://github.com/$owner/$repo.git", branch, token).awaitJson()
        val result = decodeJsonObject(json)
        if (result["ok"] != "true") {
            throw Exception(result["message"] ?: "Clone failed")
        }
        fs = instance
        currentKey = key
    }

    override suspend fun pull(): PullOutcome {
        val f = fs ?: return PullOutcome.Failed("Repository not cloned yet")
        val token = tokenProvider() ?: ""
        val json = jsPull(Git, GitHttp, f, WORK_DIR, currentBranch, token).awaitJson()
        val result = decodeJsonObject(json)
        return when (result["outcome"]) {
            "ok" -> PullOutcome.FastForwarded
            else -> PullOutcome.Failed(result["message"] ?: "Pull failed")
        }
    }

    override suspend fun readFile(path: String): String? {
        val f = fs ?: return null
        val json = jsReadFile(f, WORK_DIR, path).awaitJson()
        val result = decodeJsonObject(json)
        return if (result["found"] == "true") result["content"] else null
    }

    override suspend fun listDirectory(path: String): List<GitHubContentItem> {
        val f = fs ?: return emptyList()
        val json = jsListDirectory(f, WORK_DIR, path).awaitJson()
        return decodeJsonArray(json).map { entry ->
            GitHubContentItem(
                name = entry["name"] ?: "",
                path = entry["path"] ?: "",
                sha = "",
                type = entry["type"] ?: "file",
                size = entry["size"]?.toLongOrNull() ?: 0L,
            )
        }
    }

    override suspend fun writeAndCommit(path: String, content: String, message: String): CommitInfo {
        val f = fs ?: error("Repository not cloned yet")
        val json = jsWriteAndCommit(Git, f, WORK_DIR, path, content, message).awaitJson()
        val result = decodeJsonObject(json)
        return CommitInfo(
            sha = result["sha"] ?: "",
            message = message,
            timestampMillis = result["timestampMillis"]?.toLongOrNull() ?: 0L,
        )
    }

    override suspend fun push(): PushOutcome {
        val f = fs ?: return PushOutcome.Failed("Repository not cloned yet")
        val token = tokenProvider() ?: ""
        val json = jsPush(Git, GitHttp, f, WORK_DIR, token).awaitJson()
        val result = decodeJsonObject(json)
        return when (result["outcome"]) {
            "pushed" -> PushOutcome.Pushed
            "rejected" -> PushOutcome.Rejected(result["message"] ?: "Push rejected")
            else -> PushOutcome.Failed(result["message"] ?: "Push failed")
        }
    }

    override suspend fun unpushedCommits(): List<CommitInfo> {
        val f = fs ?: return emptyList()
        val json = jsUnpushedCommits(Git, f, WORK_DIR, currentBranch).awaitJson()
        return decodeJsonArray(json).map { entry ->
            CommitInfo(
                sha = entry["sha"] ?: "",
                message = entry["message"] ?: "",
                timestampMillis = entry["timestampMillis"]?.toLongOrNull() ?: 0L,
            )
        }
    }

    override suspend fun discardAllUnpushed() {
        val f = fs ?: return
        val json = jsDiscardAllUnpushed(Git, f, WORK_DIR, currentBranch).awaitJson()
        val result = decodeJsonObject(json)
        if (result["ok"] != "true") {
            AppLog.e("Discard all failed: ${result["message"]}")
        }
    }
}

actual fun createLocalGitRepository(tokenProvider: suspend () -> String?): LocalGitRepository =
    WasmJsLocalGitRepository(tokenProvider)
