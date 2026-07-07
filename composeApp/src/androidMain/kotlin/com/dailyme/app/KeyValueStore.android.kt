package com.dailyme.app

import android.content.Context

private const val PREFS_NAME = "dailyme_cache_prefs"

actual fun createKeyValueStore(): KeyValueStore = AndroidKeyValueStore(AndroidContextHolder.appContext)

class AndroidKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun get(key: String): String? = prefs.getString(key, null)

    override suspend fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override suspend fun keys(prefix: String): List<String> =
        prefs.all.keys.filter { it.startsWith(prefix) }
}
