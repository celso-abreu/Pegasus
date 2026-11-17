package com.flutterforge.data.remote.api

import com.flutterforge.core.network.AuthInterceptor
import com.flutterforge.data.remote.dto.BranchDto
import com.flutterforge.data.remote.dto.RateLimitResponseDto
import com.flutterforge.data.remote.dto.RepoContentDto
import com.flutterforge.data.remote.dto.RepositoryDto
import com.flutterforge.data.remote.dto.SearchRepositoriesResponseDto
import com.flutterforge.data.remote.dto.UserDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GitHubApi {

    @GET("user")
    suspend fun getCurrentUser(): UserDto

    @GET("user")
    suspend fun validateTokenUser(
        @Header(AuthInterceptor.HEADER_AUTHORIZATION) authorization: String,
        @Header(AuthInterceptor.HEADER_SKIP_AUTO_LOGOUT) skipAutoLogout: String = "true"
    ): UserDto

    @GET("user/repos")
    suspend fun getUserRepositories(
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): List<RepositoryDto>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): SearchRepositoriesResponseDto

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): List<BranchDto>

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRootContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String
    ): List<RepoContentDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getDirectoryContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String
    ): List<RepoContentDto>

    @GET("repos/{owner}/{repo}/contents/pubspec.yaml")
    suspend fun getPubspecYaml(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String? = null
    ): Response<RepoContentDto>

    @Streaming
    @GET("repos/{owner}/{repo}/zipball/{ref}")
    suspend fun downloadZipball(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): Response<ResponseBody>

    @GET("rate_limit")
    suspend fun getRateLimit(): RateLimitResponseDto
}
