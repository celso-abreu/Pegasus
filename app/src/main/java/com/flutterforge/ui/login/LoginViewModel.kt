package com.flutterforge.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.flutterforge.core.common.AppError
import com.flutterforge.core.common.Resource
import com.flutterforge.core.network.OAuthEventBus
import com.flutterforge.domain.repository.AuthRepository
import com.flutterforge.domain.usecase.ExchangeWebCodeUseCase
import com.flutterforge.domain.usecase.LoginWithPatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val patSuccess: Boolean = false,
    val isExchangingCode: Boolean = false,
    val exchangeError: AppError? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val loginWithPatUseCase: LoginWithPatUseCase,
    private val exchangeWebCodeUseCase: ExchangeWebCodeUseCase,
    private val oauthEventBus: OAuthEventBus
) : ViewModel() {

    private val sessionExpiredArg = savedStateHandle.get<Boolean>("sessionExpired") ?: false

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isClientIdConfigured = authRepository.hasClientIdConfigured(),
            sessionExpiredBanner = sessionExpiredArg
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            oauthEventBus.oauthCodes.collect { code ->
                handleOAuthCallbackCode(code)
            }
        }
    }

    fun getOAuthAuthorizeUrl(): String? {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (!authRepository.hasClientIdConfigured()) return null
        return "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=pegasus://oauth-callback&scope=repo+read:user"
    }

    private fun handleOAuthCallbackCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExchangingCode = true, exchangeError = null) }
            when (val result = exchangeWebCodeUseCase(code)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isExchangingCode = false, patSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isExchangingCode = false, exchangeError = result.error) }
                }
                Resource.Loading -> Unit
            }
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
                            patSuccess = true
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isValidatingPat = false,
                            patError = result.error
                        )
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }
}
