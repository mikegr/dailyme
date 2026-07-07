package com.dailyme.app

import kotlinx.browser.localStorage

private const val KEY_TOKEN = "dailyme_github_pat"
private const val KEY_OWNER = "dailyme_owner"
private const val KEY_REPO = "dailyme_repo"
private const val KEY_BRANCH = "dailyme_branch"

actual fun createCredentialsStore(): CredentialsStore = object : CredentialsStore {
    override suspend fun load(): StoredCredentials? {
        val token = localStorage.getItem(KEY_TOKEN) ?: return null
        val owner = localStorage.getItem(KEY_OWNER) ?: return null
        val repo = localStorage.getItem(KEY_REPO) ?: return null
        val branch = localStorage.getItem(KEY_BRANCH) ?: return null
        return StoredCredentials(token, owner, repo, branch)
    }

    override suspend fun save(credentials: StoredCredentials) {
        localStorage.setItem(KEY_TOKEN, credentials.token)
        localStorage.setItem(KEY_OWNER, credentials.owner)
        localStorage.setItem(KEY_REPO, credentials.repo)
        localStorage.setItem(KEY_BRANCH, credentials.branch)
    }

    override suspend fun clear() {
        localStorage.removeItem(KEY_TOKEN)
        localStorage.removeItem(KEY_OWNER)
        localStorage.removeItem(KEY_REPO)
        localStorage.removeItem(KEY_BRANCH)
    }
}
