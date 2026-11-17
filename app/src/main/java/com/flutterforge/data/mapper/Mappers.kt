package com.flutterforge.data.mapper

import com.flutterforge.data.local.entity.ProjectEntity
import com.flutterforge.data.remote.dto.BranchDto
import com.flutterforge.data.remote.dto.RateLimitResponseDto
import com.flutterforge.data.remote.dto.RepoContentDto
import com.flutterforge.data.remote.dto.RepositoryDto
import com.flutterforge.data.remote.dto.UserDto
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.ContentType
import com.flutterforge.domain.model.Project
import com.flutterforge.domain.model.RateLimitInfo
import com.flutterforge.domain.model.RepoContentItem
import com.flutterforge.domain.model.Repository
import com.flutterforge.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    login = login,
    name = name,
    avatarUrl = avatarUrl,
    bio = bio,
    publicRepos = publicRepos,
    totalPrivateRepos = if (totalPrivateRepos > 0) totalPrivateRepos else ownedPrivateRepos
)

fun RepositoryDto.toDomain(isFlutterProject: Boolean? = null): Repository = Repository(
    id = id,
    name = name,
    fullName = fullName,
    ownerLogin = owner.login,
    ownerAvatarUrl = owner.avatarUrl,
    description = description,
    language = language,
    starsCount = stargazersCount,
    isPrivate = isPrivate,
    defaultBranch = defaultBranch,
    updatedAtIso = updatedAt,
    htmlUrl = htmlUrl,
    isFlutterProject = isFlutterProject
)

fun BranchDto.toDomain(): Branch = Branch(
    name = name,
    commitSha = commit.sha,
    isProtected = isProtected
)

fun RepoContentDto.toDomain(): RepoContentItem {
    val contentType = when (type.lowercase()) {
        "dir" -> ContentType.DIR
        "symlink" -> ContentType.SYMLINK
        "submodule" -> ContentType.SUBMODULE
        else -> ContentType.FILE
    }
    return RepoContentItem(
        name = name,
        path = path,
        type = contentType,
        sizeBytes = size,
        sha = sha
    )
}

fun RateLimitResponseDto.toDomain(): RateLimitInfo = RateLimitInfo(
    coreLimit = resources.core.limit,
    coreRemaining = resources.core.remaining,
    coreResetEpochSeconds = resources.core.reset,
    searchLimit = resources.search.limit,
    searchRemaining = resources.search.remaining,
    searchResetEpochSeconds = resources.search.reset
)

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    owner = owner,
    repoName = repoName,
    branch = branch,
    localPath = localPath,
    sizeBytes = sizeBytes,
    importedAtMillis = importedAtMillis,
    hasPubspec = hasPubspec,
    hasMainDart = hasMainDart
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    owner = owner,
    repoName = repoName,
    branch = branch,
    localPath = localPath,
    sizeBytes = sizeBytes,
    importedAtMillis = importedAtMillis,
    hasPubspec = hasPubspec,
    hasMainDart = hasMainDart
)
