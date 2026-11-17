package com.flutterforge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: Long,
    @SerialName("login") val login: String,
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("total_private_repos") val totalPrivateRepos: Int = 0,
    @SerialName("owned_private_repos") val ownedPrivateRepos: Int = 0
)

@Serializable
data class OwnerDto(
    @SerialName("login") val login: String,
    @SerialName("avatar_url") val avatarUrl: String
)

@Serializable
data class RepositoryDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("owner") val owner: OwnerDto,
    @SerialName("description") val description: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("html_url") val htmlUrl: String
)

@Serializable
data class SearchRepositoriesResponseDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("items") val items: List<RepositoryDto>
)

@Serializable
data class BranchCommitDto(
    @SerialName("sha") val sha: String
)

@Serializable
data class BranchDto(
    @SerialName("name") val name: String,
    @SerialName("commit") val commit: BranchCommitDto,
    @SerialName("protected") val isProtected: Boolean = false
)

@Serializable
data class RepoContentDto(
    @SerialName("name") val name: String,
    @SerialName("path") val path: String,
    @SerialName("type") val type: String,
    @SerialName("size") val size: Long = 0L,
    @SerialName("sha") val sha: String = "",
    @SerialName("content") val content: String? = null,
    @SerialName("encoding") val encoding: String? = null
)

@Serializable
data class RateLimitBucketDto(
    @SerialName("limit") val limit: Int,
    @SerialName("remaining") val remaining: Int,
    @SerialName("reset") val reset: Long
)

@Serializable
data class RateLimitResourcesDto(
    @SerialName("core") val core: RateLimitBucketDto,
    @SerialName("search") val search: RateLimitBucketDto
)

@Serializable
data class RateLimitResponseDto(
    @SerialName("resources") val resources: RateLimitResourcesDto
)
