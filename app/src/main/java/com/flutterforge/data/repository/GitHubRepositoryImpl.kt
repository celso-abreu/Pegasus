package com.flutterforge.data.repository

import android.util.Base64
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.flutterforge.core.common.PubspecParser
import com.flutterforge.core.common.Resource
import com.flutterforge.core.datastore.PreferencesStorage
import com.flutterforge.core.network.ErrorHandler
import com.flutterforge.data.mapper.toDomain
import com.flutterforge.data.paging.RepositoriesPagingSource
import com.flutterforge.data.remote.api.GitHubApi
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.ContentType
import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.RateLimitInfo
import com.flutterforge.domain.model.RepoContentItem
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepositoryImpl @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val preferencesStorage: PreferencesStorage
) : GitHubRepository {

    private val flutterCache = ConcurrentHashMap<String, Boolean>()

    override suspend fun getCurrentUser(): Resource<User> {
        return try {
            val user = gitHubApi.getCurrentUser().toDomain()
            preferencesStorage.setCachedUserLogin(user.login)
            Resource.Success(user)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override fun getRepositoriesPaged(
        query: String,
        userLogin: String?,
        filter: RepoFilter
    ): Flow<PagingData<Repository>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                RepositoriesPagingSource(
                    gitHubApi = gitHubApi,
                    query = query,
                    userLogin = userLogin,
                    filter = filter,
                    flutterDetector = { owner, repo, ref ->
                        detectFlutterProject(owner, repo, ref)
                    }
                )
            }
        ).flow
    }

    override suspend fun detectFlutterProject(owner: String, repo: String, ref: String?): Boolean? {
        val key = "$owner/$repo@${ref ?: "default"}"
        flutterCache[key]?.let { return it }

        return try {
            val response = gitHubApi.getPubspecYaml(owner = owner, repo = repo, ref = ref)
            when {
                response.isSuccessful -> {
                    flutterCache[key] = true
                    true
                }
                response.code() == 404 -> {
                    flutterCache[key] = false
                    false
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun getBranches(owner: String, repo: String): Resource<List<Branch>> {
        return try {
            val branches = gitHubApi.getBranches(owner = owner, repo = repo).map { it.toDomain() }
            Resource.Success(branches)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun getRepoContents(
        owner: String,
        repo: String,
        path: String,
        ref: String
    ): Resource<List<RepoContentItem>> {
        return try {
            val dtos = if (path.isBlank()) {
                gitHubApi.getRootContents(owner = owner, repo = repo, ref = ref)
            } else {
                gitHubApi.getDirectoryContents(owner = owner, repo = repo, path = path, ref = ref)
            }
            val sorted = dtos.map { it.toDomain() }
                .sortedWith(
                    compareBy<RepoContentItem> { it.type != ContentType.DIR }
                        .thenBy { it.name.lowercase() }
                )
            Resource.Success(sorted)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun getPubspecPreview(
        owner: String,
        repo: String,
        ref: String
    ): Resource<FlutterProjectInfo?> {
        return try {
            val response = gitHubApi.getPubspecYaml(owner = owner, repo = repo, ref = ref)
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    val encoded = body?.content
                    if (encoded.isNullOrBlank()) {
                        Resource.Success(null)
                    } else {
                        val cleaned = encoded.replace("\n", "").replace("\r", "")
                        val decodedYaml = String(Base64.decode(cleaned, Base64.DEFAULT), Charsets.UTF_8)
                        val info = PubspecParser.parse(decodedYaml)
                        flutterCache["$owner/$repo@$ref"] = true
                        Resource.Success(info)
                    }
                }
                response.code() == 404 -> {
                    flutterCache["$owner/$repo@$ref"] = false
                    Resource.Success(null)
                }
                else -> {
                    Resource.Error(
                        ErrorHandler.mapHttpException(retrofit2.HttpException(response))
                    )
                }
            }
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun getRateLimit(): Resource<RateLimitInfo> {
        return try {
            Resource.Success(gitHubApi.getRateLimit().toDomain())
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override fun clearDetectionCache() {
        flutterCache.clear()
    }
}
