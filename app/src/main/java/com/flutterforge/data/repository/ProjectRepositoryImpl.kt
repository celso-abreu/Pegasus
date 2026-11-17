package com.flutterforge.data.repository

import android.content.Context
import androidx.core.content.ContextCompat
import com.flutterforge.data.local.dao.ProjectDao
import com.flutterforge.data.mapper.toDomain
import com.flutterforge.data.service.ProjectDownloadManager
import com.flutterforge.data.service.ProjectDownloadService
import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.Project
import com.flutterforge.domain.repository.ProjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val downloadManager: ProjectDownloadManager
) : ProjectRepository {

    override fun observeImportedProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDownloadState(): StateFlow<DownloadState> =
        downloadManager.downloadState

    override fun startProjectImport(owner: String, repo: String, branch: String) {
        val intent = ProjectDownloadService.createIntent(
            context = context,
            owner = owner,
            repo = repo,
            branch = branch
        )
        ContextCompat.startForegroundService(context, intent)
    }

    override suspend fun deleteProject(project: Project) {
        withContext(Dispatchers.IO) {
            val dir = File(project.localPath)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            projectDao.deleteById(project.id)
        }
    }

    override fun clearTemporaryCache(): Long {
        val cacheDir = context.cacheDir
        val freed = calculateSize(cacheDir)
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return freed
    }

    private fun calculateSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
    }
}
