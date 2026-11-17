package com.flutterforge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutterforge.core.common.Resource
import com.flutterforge.core.datastore.AppPreferences
import com.flutterforge.domain.model.RateLimitInfo
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.GitHubRepository
import com.flutterforge.domain.repository.ProjectRepository
import com.flutterforge.domain.repository.SettingsRepository
import com.flutterforge.domain.usecase.GetCurrentUserUseCase
import com.flutterforge.domain.usecase.GetRateLimitUseCase
import com.flutterforge.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getRateLimitUseCase: GetRateLimitUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository,
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = settingsRepository.observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val userState: StateFlow<Resource<User>> = _userState.asStateFlow()

    private val _rateLimitState = MutableStateFlow<Resource<RateLimitInfo>>(Resource.Loading)
    val rateLimitState: StateFlow<Resource<RateLimitInfo>> = _rateLimitState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _userState.value = Resource.Loading
            _userState.value = getCurrentUserUseCase()
        }
        refreshRateLimit()
    }

    fun refreshRateLimit() {
        viewModelScope.launch {
            _rateLimitState.value = Resource.Loading
            _rateLimitState.value = getRateLimitUseCase()
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun clearCache(): Long {
        gitHubRepository.clearDetectionCache()
        return projectRepository.clearTemporaryCache()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
