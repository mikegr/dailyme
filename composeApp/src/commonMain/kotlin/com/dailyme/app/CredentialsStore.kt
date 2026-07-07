package com.dailyme.app

data class StoredCredentials(
    val token: String,
    val owner: String,
    val repo: String,
    val branch: String,
)

interface CredentialsStore {
    suspend fun load(): StoredCredentials?
    suspend fun save(credentials: StoredCredentials)
    suspend fun clear()
}

expect fun createCredentialsStore(): CredentialsStore
