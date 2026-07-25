package com.dailyme.app

data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val type: String,
    val size: Long = 0,
)
