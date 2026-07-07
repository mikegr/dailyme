package com.dailyme.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cacheJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CachedListing(val items: List<GitHubContentItem>)

class OfflineCache(private val store: KeyValueStore) {
    private fun listingKey(owner: String, repo: String, branch: String, path: String) =
        "listing::$owner/$repo/$branch/$path"

    private fun fileKey(owner: String, repo: String, branch: String, path: String) =
        "file::$owner/$repo/$branch/$path"

    suspend fun getListing(owner: String, repo: String, branch: String, path: String): List<GitHubContentItem>? {
        val raw = store.get(listingKey(owner, repo, branch, path)) ?: return null
        return runCatching { cacheJson.decodeFromString<CachedListing>(raw).items }.getOrNull()
    }

    suspend fun putListing(owner: String, repo: String, branch: String, path: String, items: List<GitHubContentItem>) {
        store.put(listingKey(owner, repo, branch, path), cacheJson.encodeToString(CachedListing(items)))
    }

    suspend fun getFile(owner: String, repo: String, branch: String, path: String): GitHubFileContent? {
        val raw = store.get(fileKey(owner, repo, branch, path)) ?: return null
        return runCatching { cacheJson.decodeFromString<GitHubFileContent>(raw) }.getOrNull()
    }

    suspend fun putFile(owner: String, repo: String, branch: String, path: String, file: GitHubFileContent) {
        store.put(fileKey(owner, repo, branch, path), cacheJson.encodeToString(file))
    }
}
