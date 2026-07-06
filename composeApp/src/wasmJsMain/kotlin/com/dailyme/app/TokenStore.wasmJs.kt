package com.dailyme.app

import kotlinx.browser.localStorage

private const val KEY_TOKEN = "dailyme_github_pat"

actual fun createTokenStore(): TokenStore = object : TokenStore {
    override suspend fun getToken(): String? = localStorage.getItem(KEY_TOKEN)

    override suspend fun setToken(token: String?) {
        if (token == null) {
            localStorage.removeItem(KEY_TOKEN)
        } else {
            localStorage.setItem(KEY_TOKEN, token)
        }
    }
}
