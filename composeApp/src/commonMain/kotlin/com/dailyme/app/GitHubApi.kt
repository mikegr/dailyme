package com.dailyme.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val GITHUB_API_BASE = "https://api.github.com"
private val errorJson = Json { ignoreUnknownKeys = true }

class GitHubApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
) {
    suspend fun listContents(owner: String, repo: String, path: String, branch: String): List<GitHubContentItem> {
        val token = tokenProvider()
        val response = client.get("$GITHUB_API_BASE/repos/$owner/$repo/contents/$path") {
            parameter("ref", branch)
            applyAuth(token)
        }
        if (!response.status.isSuccess()) throw githubError(response.status.value, response.bodyAsText())
        return response.body()
    }

    suspend fun getFile(owner: String, repo: String, path: String, branch: String): GitHubFileContent {
        val token = tokenProvider()
        val response = client.get("$GITHUB_API_BASE/repos/$owner/$repo/contents/$path") {
            parameter("ref", branch)
            applyAuth(token)
        }
        if (!response.status.isSuccess()) throw githubError(response.status.value, response.bodyAsText())
        return response.body()
    }

    suspend fun getLatestCommitSha(owner: String, repo: String, branch: String): String {
        val token = tokenProvider()
        val response = client.get("$GITHUB_API_BASE/repos/$owner/$repo/branches/$branch") {
            applyAuth(token)
        }
        if (!response.status.isSuccess()) throw githubError(response.status.value, response.bodyAsText())
        return response.body<GitHubBranch>().commit.sha
    }

    /** Updates [path] if [previousSha] is given, or creates it if null. */
    suspend fun updateFile(
        owner: String,
        repo: String,
        path: String,
        branch: String,
        commitMessage: String,
        newContent: String,
        previousSha: String?,
    ) {
        val token = tokenProvider()
        val response = client.put("$GITHUB_API_BASE/repos/$owner/$repo/contents/$path") {
            applyAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                GitHubCommitRequest(
                    message = commitMessage,
                    content = encodeBase64(newContent),
                    sha = previousSha,
                    branch = branch,
                )
            )
        }
        if (!response.status.isSuccess()) throw githubError(response.status.value, response.bodyAsText())
    }

    private fun HttpRequestBuilder.applyAuth(token: String?) {
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
    }

    private fun githubError(status: Int, body: String): GitHubApiException {
        val message = runCatching {
            errorJson.decodeFromString(GitHubErrorResponse.serializer(), body).message
        }.getOrDefault("GitHub request failed (HTTP $status)")
        return GitHubApiException(message, status)
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64Content(content: String): String {
    val clean = content.replace("\n", "").replace("\r", "")
    return Base64.decode(clean).decodeToString()
}

@OptIn(ExperimentalEncodingApi::class)
fun encodeBase64(text: String): String {
    return Base64.encode(text.encodeToByteArray())
}
