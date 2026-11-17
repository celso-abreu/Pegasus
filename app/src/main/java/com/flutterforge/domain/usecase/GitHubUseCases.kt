package com.flutterforge.domain.usecase

import androidx.paging.PagingData
import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.RateLimitInfo
import com.flutterforge.domain.model.RepoContentItem
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(): Resource<User> = gitHubRepository.getCurrentUser()
}

class GetReposUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    operator fun invoke(
        query: String,
        userLogin: String?,
        filter: RepoFilter
    ): Flow<PagingData<Repository>> = gitHubRepository.getRepositoriesPaged(query, userLogin, filter)
}

class GetBranchesUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(owner: String, repo: String): Resource<List<Branch>> =
        gitHubRepository.getBranches(owner, repo)
}

class GetRepoContentsUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(
        owner: String,
        repo: String,
        path: String,
        ref: String
    ): Resource<List<RepoContentItem>> = gitHubRepository.getRepoContents(owner, repo, path, ref)
}

class GetPubspecPreviewUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(
        owner: String,
        repo: String,
        ref: String
    ): Resource<FlutterProjectInfo?> = gitHubRepository.getPubspecPreview(owner, repo, ref)
}

class GetRateLimitUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(): Resource<RateLimitInfo> = gitHubRepository.getRateLimit()
}
