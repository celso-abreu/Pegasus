package com.flutterforge.domain.model

enum class RepoFilter {
    ALL,
    FLUTTER_ONLY,
    PRIVATE_ONLY
}

data class Repository(
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val language: String?,
    val starsCount: Int,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val updatedAtIso: String,
    val htmlUrl: String,
    val isFlutterProject: Boolean? = null
)
