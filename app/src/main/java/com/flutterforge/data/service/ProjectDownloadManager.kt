package com.flutterforge.data.service

import com.flutterforge.domain.model.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDownloadManager @Inject constructor() {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun update(state: DownloadState) {
        _downloadState.value = state
    }

    fun reset() {
        _downloadState.value = DownloadState.Idle
    }
}
