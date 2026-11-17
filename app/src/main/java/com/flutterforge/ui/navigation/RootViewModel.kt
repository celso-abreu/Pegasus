package com.flutterforge.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutterforge.core.datastore.AppPreferences
import com.flutterforge.core.network.SessionEventBus
import com.flutterforge.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    sessionEventBus: SessionEventBus
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = settingsRepository.observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    val unauthorizedEvents: SharedFlow<Unit> = sessionEventBus.unauthorizedEvents
}
