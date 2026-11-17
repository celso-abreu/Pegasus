package com.flutterforge.domain.model

data class User(
    val id: Long,
    val login: String,
    val name: String?,
    val avatarUrl: String,
    val bio: String?,
    val publicRepos: Int,
    val totalPrivateRepos: Int
)
