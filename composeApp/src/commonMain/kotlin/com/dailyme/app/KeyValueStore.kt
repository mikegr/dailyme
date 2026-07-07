package com.dailyme.app

interface KeyValueStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun keys(prefix: String): List<String>
}

expect fun createKeyValueStore(): KeyValueStore
