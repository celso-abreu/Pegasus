package com.flutterforge.domain.usecase

import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.Project
import com.flutterforge.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ImportProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(owner: String, repo: String, branch: String) {
        projectRepository.startProjectImport(owner, repo, branch)
    }
}

class ObserveDownloadStateUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(): StateFlow<DownloadState> = projectRepository.observeDownloadState()
}

class GetImportedProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> = projectRepository.observeImportedProjects()
}

class DeleteImportedProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) = projectRepository.deleteProject(project)
}
