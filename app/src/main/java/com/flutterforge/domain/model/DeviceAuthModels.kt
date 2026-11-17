package com.flutterforge.domain.model

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

sealed interface DevicePollResult {
    data object Pending : DevicePollResult
    data class SlowDown(val newIntervalSeconds: Int) : DevicePollResult
    data object Expired : DevicePollResult
    data class Denied(val reason: String?) : DevicePollResult
    data class Success(val accessToken: String) : DevicePollResult
}

data class RateLimitInfo(
    val coreLimit: Int,
    val coreRemaining: Int,
    val coreResetEpochSeconds: Long,
    val searchLimit: Int,
    val searchRemaining: Int,
    val searchResetEpochSeconds: Long
)

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val repoFullName: String,
        val branch: String,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState

    data class Extracting(
        val repoFullName: String,
        val branch: String
    ) : DownloadState

    data class Completed(
        val project: Project
    ) : DownloadState

    data class Failed(
        val repoFullName: String,
        val message: String
    ) : DownloadState
}
