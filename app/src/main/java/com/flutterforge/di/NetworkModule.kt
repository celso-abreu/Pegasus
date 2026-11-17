package com.flutterforge.di

import com.example.BuildConfig
import com.flutterforge.core.network.AuthInterceptor
import com.flutterforge.data.remote.api.GitHubApi
import com.flutterforge.data.remote.api.GitHubAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GITHUB_AUTH_BASE_URL = "https://github.com/"
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @AuthOkHttpClient
    fun provideAuthOkHttpClient(): OkHttpClient {
        // Never log OAuth device/access token request or response bodies.
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @ApiOkHttpClient
    fun provideApiOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader(AuthInterceptor.HEADER_AUTHORIZATION)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubAuthApi(
        @AuthOkHttpClient client: OkHttpClient,
        json: Json
    ): GitHubAuthApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(GITHUB_AUTH_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GitHubAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubApi(
        @ApiOkHttpClient client: OkHttpClient,
        json: Json
    ): GitHubApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GitHubApi::class.java)
    }
}
