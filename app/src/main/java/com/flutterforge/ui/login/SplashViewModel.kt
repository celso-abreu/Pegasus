package com.flutterforge.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutterforge.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Pending : SplashDestination
    data object Home : SplashDestination
    data object Login : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Pending)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(450L)
            val hasToken = observeAuthStateUseCase().value
            _destination.value = if (hasToken) {
                SplashDestination.Home
            } else {
                SplashDestination.Login
            }
        }
    }
}
