package com.flutterforge.domain.usecase

import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.DeviceCodeInfo
import com.flutterforge.domain.model.DevicePollResult
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<Boolean> = authRepository.observeHasToken()
}

class StartDeviceFlowUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<DeviceCodeInfo> = authRepository.startDeviceFlow()
}

class PollForTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        deviceCode: String,
        currentIntervalSeconds: Int
    ): Resource<DevicePollResult> = authRepository.pollForToken(deviceCode, currentIntervalSeconds)
}

class LoginWithPatUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String): Resource<User> =
        authRepository.loginWithPersonalAccessToken(token)
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.logout()
}
