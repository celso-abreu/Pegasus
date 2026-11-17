package com.flutterforge.data.repository

import com.example.BuildConfig
import com.flutterforge.core.common.AppError
import com.flutterforge.core.common.Resource
import com.flutterforge.core.datastore.PreferencesStorage
import com.flutterforge.core.network.ErrorHandler
import com.flutterforge.core.security.TokenStorage
import com.flutterforge.data.mapper.toDomain
import com.flutterforge.data.remote.api.GitHubApi
import com.flutterforge.data.remote.api.GitHubAuthApi
import com.flutterforge.domain.model.DeviceCodeInfo
import com.flutterforge.domain.model.DevicePollResult
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val gitHubAuthApi: GitHubAuthApi,
    private val gitHubApi: GitHubApi,
    private val tokenStorage: TokenStorage,
    private val preferencesStorage: PreferencesStorage
) : AuthRepository {

    override fun observeHasToken(): StateFlow<Boolean> = tokenStorage.observeHasToken()

    override fun hasClientIdConfigured(): Boolean {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        return clientId.isNotBlank() && clientId != PLACEHOLDER_CLIENT_ID
    }

    override suspend fun startDeviceFlow(): Resource<DeviceCodeInfo> {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (!hasClientIdConfigured()) {
            return Resource.Error(AppError.Unknown("GITHUB_CLIENT_ID não configurado."))
        }

        return try {
            val dto = gitHubAuthApi.requestDeviceCode(clientId = clientId)
            val deviceCode = dto.deviceCode
            val userCode = dto.userCode
            val verificationUri = dto.verificationUri

            if (deviceCode.isNullOrBlank() || userCode.isNullOrBlank() || verificationUri.isNullOrBlank()) {
                Resource.Error(
                    AppError.Unknown(dto.errorDescription ?: dto.error ?: "Resposta inválida do GitHub")
                )
            } else {
                Resource.Success(
                    DeviceCodeInfo(
                        deviceCode = deviceCode,
                        userCode = userCode,
                        verificationUri = verificationUri,
                        expiresInSeconds = dto.expiresIn ?: 900,
                        intervalSeconds = (dto.interval ?: 5).coerceAtLeast(5)
                    )
                )
            }
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun pollForToken(
        deviceCode: String,
        currentIntervalSeconds: Int
    ): Resource<DevicePollResult> {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        return try {
            val dto = gitHubAuthApi.pollAccessToken(
                clientId = clientId,
                deviceCode = deviceCode
            )
            val token = dto.accessToken
            if (!token.isNullOrBlank()) {
                tokenStorage.saveToken(token)
                return Resource.Success(DevicePollResult.Success(token))
            }

            val result = when (dto.error) {
                "authorization_pending" -> DevicePollResult.Pending
                "slow_down" -> {
                    val serverInterval = dto.interval ?: (currentIntervalSeconds + 5)
                    DevicePollResult.SlowDown(
                        newIntervalSeconds = maxOf(currentIntervalSeconds + 5, serverInterval)
                    )
                }
                "expired_token" -> DevicePollResult.Expired
                "access_denied" -> DevicePollResult.Denied(dto.errorDescription)
                null -> DevicePollResult.Denied("Resposta inesperada sem token")
                else -> DevicePollResult.Denied(dto.errorDescription ?: dto.error)
            }
            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun exchangeWebCode(code: String): Resource<User> {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (!hasClientIdConfigured()) {
            return Resource.Error(AppError.Unknown("GITHUB_CLIENT_ID não configurado."))
        }

        return try {
            val tokenDto = gitHubAuthApi.exchangeWebCode(clientId = clientId, code = code)
            val accessToken = tokenDto.accessToken
            if (accessToken.isNullOrBlank()) {
                val errorMsg = tokenDto.errorDescription ?: tokenDto.error ?: "Falha ao obter token OAuth"
                return Resource.Error(AppError.Unknown(errorMsg))
            }

            val userDto = gitHubApi.validateTokenUser(authorization = "Bearer $accessToken")
            val user = userDto.toDomain()
            tokenStorage.saveToken(accessToken)
            preferencesStorage.setCachedUserLogin(user.login)
            Resource.Success(user)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun loginWithPersonalAccessToken(token: String): Resource<User> {
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) {
            return Resource.Error(AppError.Unknown("Token vazio"))
        }

        return try {
            val userDto = gitHubApi.validateTokenUser(authorization = "Bearer $cleanToken")
            val user = userDto.toDomain()
            tokenStorage.saveToken(cleanToken)
            preferencesStorage.setCachedUserLogin(user.login)
            Resource.Success(user)
        } catch (t: Throwable) {
            Resource.Error(ErrorHandler.mapThrowable(t))
        }
    }

    override suspend fun saveToken(token: String) {
        tokenStorage.saveToken(token)
    }

    override suspend fun logout() {
        tokenStorage.clearToken()
        preferencesStorage.clearSessionData()
    }

    private companion object {
        const val PLACEHOLDER_CLIENT_ID = "YOUR_GITHUB_OAUTH_CLIENT_ID"
    }
}
