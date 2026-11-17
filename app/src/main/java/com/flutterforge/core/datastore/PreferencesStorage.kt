package com.flutterforge.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flutterforge_prefs")

data class AppPreferences(
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = true,
    val cachedUserLogin: String? = null
)

@Singleton
class PreferencesStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            darkTheme = prefs[KEY_DARK_THEME] ?: true,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            cachedUserLogin = prefs[KEY_CACHED_LOGIN]
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCachedUserLogin(login: String?) {
        context.dataStore.edit { prefs ->
            if (login == null) prefs.remove(KEY_CACHED_LOGIN)
            else prefs[KEY_CACHED_LOGIN] = login
        }
    }

    suspend fun clearSessionData() {
        context.dataStore.edit { it.remove(KEY_CACHED_LOGIN) }
    }

    private companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_CACHED_LOGIN = stringPreferencesKey("cached_user_login")
    }
}
