package com.flutterforge.domain.repository

import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ProjectRepository {
    fun observeImportedProjects(): Flow<List<Project>>
    fun observeDownloadState(): StateFlow<DownloadState>
    fun startProjectImport(owner: String, repo: String, branch: String)
    suspend fun deleteProject(project: Project)
    fun clearTemporaryCache(): Long
}
