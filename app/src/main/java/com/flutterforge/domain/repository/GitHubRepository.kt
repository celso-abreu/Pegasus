package com.flutterforge.domain.repository

import androidx.paging.PagingData
import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.RateLimitInfo
import com.flutterforge.domain.model.RepoContentItem
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository
import com.flutterforge.domain.model.User
import kotlinx.coroutines.flow.Flow

interface GitHubRepository {
    suspend fun getCurrentUser(): Resource<User>
    fun getRepositoriesPaged(
        query: String,
        userLogin: String?,
        filter: RepoFilter
    ): Flow<PagingData<Repository>>
    suspend fun detectFlutterProject(owner: String, repo: String, ref: String? = null): Boolean?
    suspend fun getBranches(owner: String, repo: String): Resource<List<Branch>>
    suspend fun getRepoContents(owner: String, repo: String, path: String, ref: String): Resource<List<RepoContentItem>>
    suspend fun getPubspecPreview(owner: String, repo: String, ref: String): Resource<FlutterProjectInfo?>
    suspend fun getRateLimit(): Resource<RateLimitInfo>
    fun clearDetectionCache()
}
