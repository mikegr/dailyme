package com.dailyme.app

import kotlinx.serialization.Serializable

@Serializable
data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val type: String,
    val size: Long = 0,
)

@Serializable
data class GitHubFileContent(
    val name: String,
    val path: String,
    val sha: String,
    val content: String? = null,
    val encoding: String? = null,
)

@Serializable
data class GitHubCommitRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String? = null,
)

@Serializable
data class GitHubErrorResponse(
    val message: String = "Unknown error",
)

@Serializable
data class GitHubBranch(val commit: GitHubBranchCommit)

@Serializable
data class GitHubBranchCommit(val sha: String)

class GitHubApiException(message: String, val statusCode: Int) : Exception(message)
