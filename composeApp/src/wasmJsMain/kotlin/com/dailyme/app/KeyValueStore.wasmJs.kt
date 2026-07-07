package com.dailyme.app

import kotlinx.browser.localStorage

private const val PREFIX = "dailyme_cache_"

actual fun createKeyValueStore(): KeyValueStore = object : KeyValueStore {
    override suspend fun get(key: String): String? = localStorage.getItem(PREFIX + key)

    override suspend fun put(key: String, value: String) {
        localStorage.setItem(PREFIX + key, value)
    }

    override suspend fun remove(key: String) {
        localStorage.removeItem(PREFIX + key)
    }

    override suspend fun keys(prefix: String): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val storedKey = localStorage.key(i) ?: continue
            if (storedKey.startsWith(PREFIX)) {
                val key = storedKey.removePrefix(PREFIX)
                if (key.startsWith(prefix)) result.add(key)
            }
        }
        return result
    }
}
