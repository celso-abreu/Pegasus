package com.flutterforge.domain.repository

import com.flutterforge.core.datastore.AppPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observePreferences(): Flow<AppPreferences>
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setDynamicColor(enabled: Boolean)
}
