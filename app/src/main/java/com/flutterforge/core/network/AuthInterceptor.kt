package com.flutterforge.core.network

import com.flutterforge.core.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val sessionEventBus: SessionEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val hasExplicitAuth = original.header(HEADER_AUTHORIZATION) != null
        val skipAutoLogout = original.header(HEADER_SKIP_AUTO_LOGOUT) != null

        val builder = original.newBuilder()
            .removeHeader(HEADER_SKIP_AUTO_LOGOUT)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "FlutterForge-Android")

        if (!hasExplicitAuth) {
            val token = tokenStorage.getToken()
            if (!token.isNullOrBlank()) {
                builder.header(HEADER_AUTHORIZATION, "Bearer $token")
            }
        }

        val response = chain.proceed(builder.build())

        if (response.code == 401 && !skipAutoLogout && !tokenStorage.getToken().isNullOrBlank()) {
            tokenStorage.clearToken()
            sessionEventBus.notifyUnauthorized()
        }

        return response
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_SKIP_AUTO_LOGOUT = "X-FlutterForge-Skip-Auto-Logout"
    }
}
