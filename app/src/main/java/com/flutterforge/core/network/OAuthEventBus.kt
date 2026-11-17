package com.flutterforge.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OAuthEventBus @Inject constructor() {
    private val _oauthCodes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val oauthCodes: SharedFlow<String> = _oauthCodes.asSharedFlow()

    fun emitCode(code: String) {
        _oauthCodes.tryEmit(code)
    }
}
