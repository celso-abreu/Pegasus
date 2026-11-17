package com.flutterforge.domain.model

enum class ContentType {
    FILE,
    DIR,
    SYMLINK,
    SUBMODULE
}

data class RepoContentItem(
    val name: String,
    val path: String,
    val type: ContentType,
    val sizeBytes: Long,
    val sha: String
)
