package com.flutterforge.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.Project
import com.flutterforge.domain.usecase.DeleteImportedProjectUseCase
import com.flutterforge.domain.usecase.GetImportedProjectsUseCase
import com.flutterforge.domain.usecase.ObserveDownloadStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    getImportedProjectsUseCase: GetImportedProjectsUseCase,
    observeDownloadStateUseCase: ObserveDownloadStateUseCase,
    private val deleteImportedProjectUseCase: DeleteImportedProjectUseCase
) : ViewModel() {

    val projects: StateFlow<List<Project>> = getImportedProjectsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val downloadState: StateFlow<DownloadState> = observeDownloadStateUseCase()

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            deleteImportedProjectUseCase(project)
        }
    }
}
