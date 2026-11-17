package com.flutterforge.domain.usecase

import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.AuthRepository
import javax.inject.Inject

class ExchangeWebCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Resource<User> = authRepository.exchangeWebCode(code)
}
