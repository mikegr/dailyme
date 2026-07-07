package com.dailyme.app

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "dailyme_secure_prefs"
private const val KEY_TOKEN = "github_pat"
private const val KEY_OWNER = "owner"
private const val KEY_REPO = "repo"
private const val KEY_BRANCH = "branch"

actual fun createCredentialsStore(): CredentialsStore = AndroidCredentialsStore(AndroidContextHolder.appContext)

class AndroidCredentialsStore(private val context: android.content.Context) : CredentialsStore {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun load(): StoredCredentials? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val owner = prefs.getString(KEY_OWNER, null) ?: return null
        val repo = prefs.getString(KEY_REPO, null) ?: return null
        val branch = prefs.getString(KEY_BRANCH, null) ?: return null
        return StoredCredentials(token, owner, repo, branch)
    }

    override suspend fun save(credentials: StoredCredentials) {
        prefs.edit()
            .putString(KEY_TOKEN, credentials.token)
            .putString(KEY_OWNER, credentials.owner)
            .putString(KEY_REPO, credentials.repo)
            .putString(KEY_BRANCH, credentials.branch)
            .apply()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }
}
