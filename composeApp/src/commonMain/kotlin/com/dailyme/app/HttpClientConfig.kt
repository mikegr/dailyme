package com.dailyme.app

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.installDailyMeDefaults() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                // Omit null fields (e.g. a create-file commit's absent `sha`) from request bodies,
                // since GitHub's contents API treats a present-but-null sha differently from an absent one.
                explicitNulls = false
            }
        )
    }
}
