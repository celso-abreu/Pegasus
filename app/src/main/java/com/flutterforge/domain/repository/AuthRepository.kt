package com.flutterforge.domain.repository

import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.DeviceCodeInfo
import com.flutterforge.domain.model.DevicePollResult
import com.flutterforge.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    fun observeHasToken(): StateFlow<Boolean>
    fun hasClientIdConfigured(): Boolean
    suspend fun startDeviceFlow(): Resource<DeviceCodeInfo>
    suspend fun pollForToken(deviceCode: String, currentIntervalSeconds: Int): Resource<DevicePollResult>
    suspend fun loginWithPersonalAccessToken(token: String): Resource<User>
    suspend fun saveToken(token: String)
    suspend fun logout()
}
