package com.flutterforge.data.repository

import com.flutterforge.core.datastore.AppPreferences
import com.flutterforge.core.datastore.PreferencesStorage
import com.flutterforge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesStorage: PreferencesStorage
) : SettingsRepository {

    override fun observePreferences(): Flow<AppPreferences> = preferencesStorage.preferencesFlow

    override suspend fun setDarkTheme(enabled: Boolean) {
        preferencesStorage.setDarkTheme(enabled)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        preferencesStorage.setDynamicColor(enabled)
    }
}
