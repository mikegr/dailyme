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
}
