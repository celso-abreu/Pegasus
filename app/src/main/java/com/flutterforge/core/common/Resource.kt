package com.flutterforge.core.common

sealed interface AppError {
    data object NoInternet : AppError
    data object Unauthorized : AppError
    data class RateLimit(val resetAtEpochSeconds: Long?) : AppError
    data object NotFound : AppError
    data class ServerError(val code: Int) : AppError
    data object Timeout : AppError
    data class Forbidden(val message: String?) : AppError
    data class Unknown(val message: String?) : AppError
}

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val error: AppError) : Resource<Nothing>
}
