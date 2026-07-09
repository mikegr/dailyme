package com.dailyme.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cacheJson = Json { ignoreUnknownKeys = true }

private const val LISTING_PREFIX = "listing::"
private const val FILE_PREFIX = "file::"
private const val COMMIT_SHA_PREFIX = "commit_sha::"

@Serializable
private data class CachedListing(val items: List<GitHubContentItem>)

class OfflineCache(private val store: KeyValueStore) {
    private fun listingKey(owner: String, repo: String, branch: String, path: String) =
        "$LISTING_PREFIX$owner/$repo/$branch/$path"

    private fun fileKey(owner: String, repo: String, branch: String, path: String) =
        "$FILE_PREFIX$owner/$repo/$branch/$path"

    private fun commitShaKey(owner: String, repo: String, branch: String) =
        "$COMMIT_SHA_PREFIX$owner/$repo/$branch"

    suspend fun getListing(owner: String, repo: String, branch: String, path: String): List<GitHubContentItem>? {
        val raw = store.get(listingKey(owner, repo, branch, path)) ?: return null
        return runCatching { cacheJson.decodeFromString<CachedListing>(raw).items }.getOrNull()
    }

    suspend fun putListing(owner: String, repo: String, branch: String, path: String, items: List<GitHubContentItem>) {
        store.put(listingKey(owner, repo, branch, path), cacheJson.encodeToString(CachedListing(items)))
    }

    /** Drops the cached listing for a single folder, e.g. after saving a file into it. */
    suspend fun removeListing(owner: String, repo: String, branch: String, path: String) {
        store.remove(listingKey(owner, repo, branch, path))
    }

    suspend fun getFile(owner: String, repo: String, branch: String, path: String): GitHubFileContent? {
        val raw = store.get(fileKey(owner, repo, branch, path)) ?: return null
        return runCatching { cacheJson.decodeFromString<GitHubFileContent>(raw) }.getOrNull()
    }

    suspend fun putFile(owner: String, repo: String, branch: String, path: String, file: GitHubFileContent) {
        store.put(fileKey(owner, repo, branch, path), cacheJson.encodeToString(file))
    }

    suspend fun getLastKnownCommitSha(owner: String, repo: String, branch: String): String? =
        store.get(commitShaKey(owner, repo, branch))

    suspend fun setLastKnownCommitSha(owner: String, repo: String, branch: String, sha: String) {
        store.put(commitShaKey(owner, repo, branch), sha)
    }

    /** Wipes all cached listings and files (but not the last-known commit SHA). */
    suspend fun clear() {
        for (key in store.keys(LISTING_PREFIX) + store.keys(FILE_PREFIX)) {
            store.remove(key)
        }
    }
}
