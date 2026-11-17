package com.flutterforge.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.flutterforge.core.common.AppError
import com.flutterforge.core.network.ErrorHandler
import com.flutterforge.data.mapper.toDomain
import com.flutterforge.data.remote.api.GitHubApi
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class AppErrorException(val appError: AppError) : Exception(appError.toString())

class RepositoriesPagingSource(
    private val gitHubApi: GitHubApi,
    private val query: String,
    private val userLogin: String?,
    private val filter: RepoFilter,
    private val flutterDetector: suspend (owner: String, repo: String, ref: String) -> Boolean?
) : PagingSource<Int, Repository>() {

    override fun getRefreshKey(state: PagingState<Int, Repository>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repository> {
        val startPage = params.key ?: 1
        val pageSize = params.loadSize.coerceIn(1, 50)

        return try {
            var currentPage = startPage
            val collected = mutableListOf<Repository>()
            var reachedEnd = false
            var pagesScanned = 0

            // For filtered tabs (FLUTTER_ONLY / PRIVATE_ONLY), scan up to 3 pages per load
            // so the user doesn't get an empty first page when matches exist further down.
            val maxScanPages = if (filter == RepoFilter.ALL) 1 else 3

            while (pagesScanned < maxScanPages) {
                val rawDtos = if (query.isBlank()) {
                    gitHubApi.getUserRepositories(perPage = pageSize, page = currentPage)
                } else {
                    val searchQuery = if (!userLogin.isNullOrBlank()) {
                        "${query.trim()} user:$userLogin"
                    } else {
                        query.trim()
                    }
                    gitHubApi.searchRepositories(
                        query = searchQuery,
                        perPage = pageSize,
                        page = currentPage
                    ).items
                }

                if (rawDtos.isEmpty()) {
                    reachedEnd = true
                    break
                }

                val mapped = when (filter) {
                    RepoFilter.ALL -> rawDtos.map { it.toDomain() }
                    RepoFilter.PRIVATE_ONLY -> rawDtos
                        .filter { it.isPrivate }
                        .map { it.toDomain() }
                    RepoFilter.FLUTTER_ONLY -> filterFlutterRepos(rawDtos.map { it.toDomain() })
                }

                collected.addAll(mapped)
                pagesScanned++

                if (rawDtos.size < pageSize) {
                    reachedEnd = true
                    break
                }

                if (collected.isNotEmpty()) {
                    break
                }
                currentPage++
            }

            val nextKey = if (reachedEnd) null else currentPage + 1
            val prevKey = if (startPage == 1) null else startPage - 1

            LoadResult.Page(
                data = collected,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (t: Throwable) {
            LoadResult.Error(AppErrorException(ErrorHandler.mapThrowable(t)))
        }
    }

    private suspend fun filterFlutterRepos(repos: List<Repository>): List<Repository> = coroutineScope {
        val semaphore = Semaphore(4)
        repos.map { repo ->
            async {
                semaphore.withPermit {
                    val isFlutter = flutterDetector(repo.ownerLogin, repo.name, repo.defaultBranch)
                    if (isFlutter == true) repo.copy(isFlutterProject = true) else null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
