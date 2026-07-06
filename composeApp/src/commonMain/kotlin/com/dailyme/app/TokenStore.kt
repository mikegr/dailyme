package com.dailyme.app

interface TokenStore {
    suspend fun getToken(): String?
    suspend fun setToken(token: String?)
}

expect fun createTokenStore(): TokenStore
