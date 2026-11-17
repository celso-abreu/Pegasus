package com.flutterforge.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.flutterforge.core.common.DateTimeUtils
import com.flutterforge.core.common.PubspecParser
import com.flutterforge.core.common.ZipExtractor
import com.flutterforge.data.local.dao.ProjectDao
import com.flutterforge.data.mapper.toEntity
import com.flutterforge.di.ApiOkHttpClient
import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.Project
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ProjectDownloadService : Service() {

    @Inject
    @ApiOkHttpClient
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var projectDao: ProjectDao

    @Inject
    lateinit var downloadManager: ProjectDownloadManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val owner = intent?.getStringExtra(EXTRA_OWNER)
        val repo = intent?.getStringExtra(EXTRA_REPO)
        val branch = intent?.getStringExtra(EXTRA_BRANCH)

        if (owner.isNullOrBlank() || repo.isNullOrBlank() || branch.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val repoFullName = "$owner/$repo"
        val initialNotification = buildProgressNotification(
            title = getString(R.string.notif_downloading_title, repoFullName),
            text = getString(R.string.notif_downloading_text, "0 B"),
            progress = 0,
            max = 100,
            indeterminate = true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        activeJob?.cancel()
        activeJob = serviceScope.launch {
            executeImport(owner = owner, repo = repo, branch = branch, startId = startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun executeImport(owner: String, repo: String, branch: String, startId: Int) {
        val repoFullName = "$owner/$repo"
        val tempZip = File(cacheDir, "import_${sanitizeSegment(owner)}_${sanitizeSegment(repo)}.zip")
        val projectsRoot = File(filesDir, "projects")
        val folderName = "${sanitizeSegment(owner)}_${sanitizeSegment(repo)}_${sanitizeSegment(branch)}"
        val destDir = File(projectsRoot, folderName)

        try {
            downloadManager.update(
                DownloadState.Downloading(
                    repoFullName = repoFullName,
                    branch = branch,
                    bytesDownloaded = 0L,
                    totalBytes = -1L
                )
            )

            val url = "https://api.github.com/repos/$owner/$repo/zipball/$branch"
            val request = Request.Builder().url(url).get().build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var lastNotifyTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempZip).use { output ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastNotifyTime >= 250L) {
                                lastNotifyTime = now
                                downloadManager.update(
                                    DownloadState.Downloading(
                                        repoFullName = repoFullName,
                                        branch = branch,
                                        bytesDownloaded = downloaded,
                                        totalBytes = totalBytes
                                    )
                                )
                                updateProgressNotification(repoFullName, downloaded, totalBytes)
                            }
                        }
                    }
                }
            }

            downloadManager.update(
                DownloadState.Extracting(repoFullName = repoFullName, branch = branch)
            )
            notifyManager().notify(
                NOTIFICATION_ID,
                buildProgressNotification(
                    title = getString(R.string.notif_downloading_title, repoFullName),
                    text = getString(R.string.notif_extracting_text),
                    progress = 0,
                    max = 0,
                    indeterminate = true
                )
            )

            val extraction = ZipExtractor.extractSafely(tempZip, destDir)

            val pubspecFile = File(destDir, "pubspec.yaml")
            val parsedName = if (pubspecFile.isFile) {
                runCatching { PubspecParser.parse(pubspecFile.readText()).name }.getOrNull()
            } else null

            val project = Project(
                id = "${owner}_${repo}_$branch",
                name = parsedName?.takeIf { it.isNotBlank() } ?: repo,
                owner = owner,
                repoName = repo,
                branch = branch,
                localPath = destDir.absolutePath,
                sizeBytes = extraction.totalSizeBytes,
                importedAtMillis = System.currentTimeMillis(),
                hasPubspec = extraction.hasPubspecYaml,
                hasMainDart = extraction.hasLibMainDart
            )

            projectDao.upsert(project.toEntity())
            downloadManager.update(DownloadState.Completed(project))

            notifyManager().notify(
                RESULT_NOTIFICATION_ID,
                buildResultNotification(
                    title = getString(R.string.notif_completed_title, project.name),
                    text = getString(
                        R.string.notif_completed_text,
                        DateTimeUtils.formatBytes(project.sizeBytes)
                    )
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val message = t.localizedMessage ?: "Erro desconhecido"
            downloadManager.update(DownloadState.Failed(repoFullName = repoFullName, message = message))
            notifyManager().notify(
                RESULT_NOTIFICATION_ID,
                buildResultNotification(
                    title = getString(R.string.notif_failed_title, repoFullName),
                    text = message
                )
            )
        } finally {
            if (tempZip.exists()) tempZip.delete()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    private fun updateProgressNotification(repoFullName: String, downloaded: Long, totalBytes: Long) {
        val notification = if (totalBytes > 0L) {
            val percent = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
            buildProgressNotification(
                title = getString(R.string.notif_downloading_title, repoFullName),
                text = "${DateTimeUtils.formatBytes(downloaded)} / ${DateTimeUtils.formatBytes(totalBytes)}",
                progress = percent,
                max = 100,
                indeterminate = false
            )
        } else {
            buildProgressNotification(
                title = getString(R.string.notif_downloading_title, repoFullName),
                text = getString(R.string.notif_downloading_text, DateTimeUtils.formatBytes(downloaded)),
                progress = 0,
                max = 100,
                indeterminate = true
            )
        }
        notifyManager().notify(NOTIFICATION_ID, notification)
    }

    private fun buildProgressNotification(
        title: String,
        text: String,
        progress: Int,
        max: Int,
        indeterminate: Boolean
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, indeterminate)
            .setContentIntent(createLaunchPendingIntent())
            .build()
    }

    private fun buildResultNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(createLaunchPendingIntent())
            .build()
    }

    private fun createLaunchPendingIntent(): PendingIntent {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        notifyManager().createNotificationChannel(channel)
    }

    private fun notifyManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun sanitizeSegment(value: String): String =
        value.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "flutterforge_project_imports"
        private const val NOTIFICATION_ID = 4101
        private const val RESULT_NOTIFICATION_ID = 4102

        private const val EXTRA_OWNER = "extra_owner"
        private const val EXTRA_REPO = "extra_repo"
        private const val EXTRA_BRANCH = "extra_branch"

        fun createIntent(context: Context, owner: String, repo: String, branch: String): Intent {
            return Intent(context, ProjectDownloadService::class.java).apply {
                putExtra(EXTRA_OWNER, owner)
                putExtra(EXTRA_REPO, repo)
                putExtra(EXTRA_BRANCH, branch)
            }
        }
    }
}
