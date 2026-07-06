package com.dailyme.app

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "dailyme_secure_prefs"
private const val KEY_TOKEN = "github_pat"

actual fun createTokenStore(): TokenStore = AndroidTokenStore(AndroidContextHolder.appContext)

class AndroidTokenStore(private val context: android.content.Context) : TokenStore {
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

    override suspend fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override suspend fun setToken(token: String?) {
        prefs.edit().apply {
            if (token == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }.apply()
    }
}
