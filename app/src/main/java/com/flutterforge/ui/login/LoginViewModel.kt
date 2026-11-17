package com.flutterforge.ui.login

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutterforge.core.common.AppError
import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.DeviceCodeInfo
import com.flutterforge.domain.model.DevicePollResult
import com.flutterforge.domain.repository.AuthRepository
import com.flutterforge.domain.usecase.LoginWithPatUseCase
import com.flutterforge.domain.usecase.PollForTokenUseCase
import com.flutterforge.domain.usecase.StartDeviceFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isClientIdConfigured: Boolean = true,
    val sessionExpiredBanner: Boolean = false,
    val showPatSheet: Boolean = false,
    val patInput: String = "",
    val isValidatingPat: Boolean = false,
    val patError: AppError? = null,
    val loginSuccess: Boolean = false,
    val isStartingDeviceFlow: Boolean = false,
    val deviceCodeInfo: DeviceCodeInfo? = null,
    val deviceCodeSecondsRemaining: Int = 0,
    val isPollingForDeviceAuthorization: Boolean = false,
    val deviceFlowError: AppError? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val loginWithPatUseCase: LoginWithPatUseCase,
    private val startDeviceFlowUseCase: StartDeviceFlowUseCase,
    private val pollForTokenUseCase: PollForTokenUseCase
) : ViewModel() {

    private val sessionExpiredArg = savedStateHandle.get<Boolean>("sessionExpired") ?: false
    private val _uiState = MutableStateFlow(
        LoginUiState(
            isClientIdConfigured = authRepository.hasClientIdConfigured(),
            sessionExpiredBanner = sessionExpiredArg
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var deviceFlowJob: Job? = null

    fun connectWithGitHub() {
        if (deviceFlowJob?.isActive == true || !_uiState.value.isClientIdConfigured) return

        deviceFlowJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStartingDeviceFlow = true,
                    deviceCodeInfo = null,
                    deviceCodeSecondsRemaining = 0,
                    isPollingForDeviceAuthorization = false,
                    deviceFlowError = null
                )
            }

            when (val result = startDeviceFlowUseCase()) {
                is Resource.Success -> runDeviceFlow(result.data)
                is Resource.Error -> _uiState.update {
                    it.copy(isStartingDeviceFlow = false, deviceFlowError = result.error)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private suspend fun runDeviceFlow(info: DeviceCodeInfo) {
        val now = SystemClock.elapsedRealtime()
        val expiresAt = now + info.expiresInSeconds.coerceAtLeast(1) * 1_000L
        var intervalSeconds = info.intervalSeconds.coerceAtLeast(5)
        var nextPollAt = now + intervalSeconds * 1_000L

        _uiState.update {
            it.copy(
                isStartingDeviceFlow = false,
                deviceCodeInfo = info,
                deviceCodeSecondsRemaining = info.expiresInSeconds.coerceAtLeast(1),
                isPollingForDeviceAuthorization = true,
                deviceFlowError = null
            )
        }

        while (viewModelScope.coroutineContext.isActive) {
            val currentTime = SystemClock.elapsedRealtime()
            val remaining = ((expiresAt - currentTime + 999L) / 1_000L).toInt().coerceAtLeast(0)
            _uiState.update { it.copy(deviceCodeSecondsRemaining = remaining) }

            if (remaining == 0) {
                _uiState.update {
                    it.copy(
                        isPollingForDeviceAuthorization = false,
                        deviceFlowError = AppError.Unknown("O código de autorização expirou.")
                    )
                }
                return
            }

            if (currentTime >= nextPollAt) {
                when (val result = pollForTokenUseCase(info.deviceCode, intervalSeconds)) {
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isPollingForDeviceAuthorization = false, deviceFlowError = result.error)
                        }
                        return
                    }
                    is Resource.Success -> when (val pollResult = result.data) {
                        DevicePollResult.Pending -> nextPollAt = SystemClock.elapsedRealtime() + intervalSeconds * 1_000L
                        is DevicePollResult.SlowDown -> {
                            intervalSeconds = pollResult.newIntervalSeconds.coerceAtLeast(intervalSeconds + 5)
                            nextPollAt = SystemClock.elapsedRealtime() + intervalSeconds * 1_000L
                        }
                        DevicePollResult.Expired -> {
                            _uiState.update {
                                it.copy(
                                    isPollingForDeviceAuthorization = false,
                                    deviceCodeSecondsRemaining = 0,
                                    deviceFlowError = AppError.Unknown("O código de autorização expirou.")
                                )
                            }
                            return
                        }
                        is DevicePollResult.Denied -> {
                            _uiState.update {
                                it.copy(
                                    isPollingForDeviceAuthorization = false,
                                    deviceFlowError = AppError.Unknown(
                                        pollResult.reason ?: "A autorização foi negada no GitHub."
                                    )
                                )
                            }
                            return
                        }
                        is DevicePollResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isPollingForDeviceAuthorization = false,
                                    deviceCodeInfo = null,
                                    deviceFlowError = null,
                                    loginSuccess = true
                                )
                            }
                            return
                        }
                    }
                    Resource.Loading -> Unit
                }
            }
            delay(500L)
        }
    }

    fun cancelDeviceFlow() {
        deviceFlowJob?.cancel()
        deviceFlowJob = null
        _uiState.update {
            it.copy(
                isStartingDeviceFlow = false,
                deviceCodeInfo = null,
                deviceCodeSecondsRemaining = 0,
                isPollingForDeviceAuthorization = false,
                deviceFlowError = null
            )
        }
    }

    fun openPatSheet() {
        _uiState.update { it.copy(showPatSheet = true, patError = null) }
    }

    fun closePatSheet() {
        _uiState.update { it.copy(showPatSheet = false, patError = null) }
    }

    fun onPatInputChanged(value: String) {
        _uiState.update { it.copy(patInput = value, patError = null) }
    }

    fun dismissSessionExpiredBanner() {
        _uiState.update { it.copy(sessionExpiredBanner = false) }
    }

    fun submitPat() {
        val token = _uiState.value.patInput.trim()
        if (token.isEmpty()) {
            _uiState.update { it.copy(patError = AppError.Unknown("Informe um token válido")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingPat = true, patError = null) }
            when (val result = loginWithPatUseCase(token)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isValidatingPat = false,
                            showPatSheet = false,
                            patInput = "",
                            loginSuccess = true
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isValidatingPat = false, patError = result.error) }
                }
                Resource.Loading -> Unit
            }
        }
    }
}
