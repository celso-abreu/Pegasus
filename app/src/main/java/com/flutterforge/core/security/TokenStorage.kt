package com.flutterforge.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION")
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)
    private val _hasToken = MutableStateFlow(!getToken().isNullOrBlank())

    fun observeHasToken(): StateFlow<Boolean> = _hasToken.asStateFlow()

    fun saveToken(token: String) {
        val sanitized = token.trim()
        prefs.edit().putString(KEY_GITHUB_TOKEN, sanitized).commit()
        _hasToken.value = sanitized.isNotEmpty()
    }

    fun getToken(): String? = prefs.getString(KEY_GITHUB_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_GITHUB_TOKEN).commit()
        _hasToken.value = false
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return runCatching {
            buildEncryptedPrefs(context)
        }.getOrElse {
            // If Android Keystore key was invalidated (e.g. restore), reset encrypted prefs file.
            context.deleteSharedPreferences(PREFS_NAME)
            buildEncryptedPrefs(context)
        }
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private companion object {
        const val PREFS_NAME = "flutterforge_secure_prefs"
        const val KEY_GITHUB_TOKEN = "github_access_token"
    }
}
