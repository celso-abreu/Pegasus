package com.flutterforge.core.network

import com.flutterforge.core.common.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {

    fun mapThrowable(throwable: Throwable): AppError {
        return when (throwable) {
            is HttpException -> mapHttpException(throwable)
            is SocketTimeoutException -> AppError.Timeout
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException -> AppError.NoInternet
            is IOException -> AppError.NoInternet
            else -> AppError.Unknown(throwable.localizedMessage)
        }
    }

    fun mapHttpException(exception: HttpException): AppError {
        val code = exception.code()
        val response = exception.response()
        val headers = response?.headers()

        return when (code) {
            401 -> AppError.Unauthorized
            403 -> {
                val remaining = headers?.get("X-RateLimit-Remaining")?.toIntOrNull()
                val resetEpoch = headers?.get("X-RateLimit-Reset")?.toLongOrNull()
                if (remaining == 0 || resetEpoch != null) {
                    AppError.RateLimit(resetEpoch)
                } else {
                    AppError.Forbidden(exception.message())
                }
            }
            404 -> AppError.NotFound
            in 500..599 -> AppError.ServerError(code)
            else -> AppError.Unknown("HTTP $code")
        }
    }
}
