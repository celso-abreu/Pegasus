package com.flutterforge.ui.repodetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.flutterforge.core.common.DateTimeUtils
import com.flutterforge.core.common.Resource
import com.flutterforge.core.ui.components.EmptyStateView
import com.flutterforge.core.ui.components.ErrorStateView
import com.flutterforge.core.ui.components.FlutterBadge
import com.flutterforge.core.ui.components.PrivateBadge
import com.flutterforge.core.ui.theme.JetBrainsMonoFontFamily
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.ContentType
import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.RepoContentItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepoDetailScreen(
    onBack: () -> Unit,
    onOpenProjects: () -> Unit,
    viewModel: RepoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = uiState.currentPath.isNotEmpty()) {
        viewModel.navigateUpDirectory()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startImport()
    }

    val repoFullName = "${uiState.owner}/${uiState.repoName}"
    val isThisRepoDownloading = when (val state = downloadState) {
        is DownloadState.Downloading -> state.repoFullName == repoFullName
        is DownloadState.Extracting -> state.repoFullName == repoFullName
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.repoName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = repoFullName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!viewModel.navigateUpDirectory()) onBack()
                        },
                        modifier = Modifier.testTag("repo_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isPrivate) {
                        PrivateBadge(modifier = Modifier.padding(end = 12.dp))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.startImport()
                            }
                        },
                        enabled = !isThisRepoDownloading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_project_button"),
                        shape = MaterialTheme.shapes.small
                    ) {
                        if (isThisRepoDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.repo_detail_importing))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.repo_detail_import))
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::openApkSoonSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("build_apk_button"),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.repo_detail_build_apk))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.description.isNullOrBlank()) {
                    item {
                        Text(
                            text = uiState.description!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live download banner when importing
                when (val dl = downloadState) {
                    is DownloadState.Downloading if dl.repoFullName == repoFullName -> {
                        item {
                            DownloadProgressCard(
                                title = stringResource(R.string.repo_detail_importing),
                                subtitle = if (dl.totalBytes > 0) {
                                    stringResource(
                                        R.string.repo_detail_download_progress,
                                        DateTimeUtils.formatBytes(dl.bytesDownloaded),
                                        DateTimeUtils.formatBytes(dl.totalBytes)
                                    )
                                } else {
                                    stringResource(
                                        R.string.repo_detail_download_indeterminate,
                                        DateTimeUtils.formatBytes(dl.bytesDownloaded)
                                    )
                                },
                                progress = if (dl.totalBytes > 0) {
                                    (dl.bytesDownloaded.toFloat() / dl.totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else null
                            )
                        }
                    }

                    is DownloadState.Extracting if dl.repoFullName == repoFullName -> {
                        item {
                            DownloadProgressCard(
                                title = stringResource(R.string.repo_detail_importing),
                                subtitle = stringResource(R.string.repo_detail_extracting),
                                progress = null
                            )
                        }
                    }

                    is DownloadState.Completed if "${dl.project.owner}/${dl.project.repoName}" == repoFullName -> {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.repo_detail_import_success),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onOpenProjects) {
                                        Text(text = stringResource(R.string.nav_projects))
                                    }
                                }
                            }
                        }
                    }

                    is DownloadState.Failed if dl.repoFullName == repoFullName -> {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = dl.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    else -> Unit
                }

                // Branch Selector
                item {
                    BranchSelectorDropdown(
                        selectedBranch = uiState.selectedBranch,
                        branchesState = uiState.branchesState,
                        onSelectBranch = viewModel::onBranchSelected
                    )
                }

                // Pubspec.yaml Preview
                item {
                    PubspecPreviewCard(pubspecState = uiState.pubspecState)
                }

                // File Tree Header + Breadcrumb
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.repo_detail_files),
                            style = MaterialTheme.typography.titleSmall
                        )
                        BreadcrumbBar(
                            currentPath = uiState.currentPath,
                            onNavigate = viewModel::navigateToDirectory
                        )
                    }
                }

                // File Tree Contents
                when (val contents = uiState.contentsState) {
                    Resource.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    is Resource.Error -> {
                        item {
                            ErrorStateView(
                                error = contents.error,
                                onRetry = viewModel::reloadContents
                            )
                        }
                    }

                    is Resource.Success -> {
                        if (contents.data.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Rounded.Folder,
                                    title = stringResource(R.string.repo_detail_empty_dir),
                                    subtitle = ""
                                )
                            }
                        } else {
                            items(contents.data, key = { it.path }) { item ->
                                ContentRow(
                                    item = item,
                                    onClick = {
                                        if (item.type == ContentType.DIR) {
                                            viewModel.navigateToDirectory(item.path)
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showApkSoonSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeApkSoonSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(R.string.repo_detail_apk_soon_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.repo_detail_apk_soon_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = viewModel::closeApkSoonSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchSelectorDropdown(
    selectedBranch: String,
    branchesState: Resource<List<Branch>>,
    onSelectBranch: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val branches = (branchesState as? Resource.Success)?.data.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (branches.isNotEmpty()) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBranch,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.repo_detail_branch)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("branch_dropdown")
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectBranch(branch.name)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PubspecPreviewCard(pubspecState: Resource<FlutterProjectInfo?>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.repo_detail_pubspec_title),
                style = MaterialTheme.typography.titleSmall
            )
            if ((pubspecState as? Resource.Success)?.data != null) {
                FlutterBadge()
            }
        }

        when (pubspecState) {
            Resource.Loading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is Resource.Error -> {
                Text(
                    text = stringResource(R.string.repo_detail_not_flutter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is Resource.Success -> {
                val info = pubspecState.data
                if (info == null) {
                    Text(
                        text = stringResource(R.string.repo_detail_not_flutter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = info.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!info.description.isNullOrBlank()) {
                                Text(
                                    text = info.description!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                info.version?.let { ver ->
                                    Text(
                                        text = "v$ver",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                info.sdkConstraint?.let { sdk ->
                                    Text(
                                        text = "SDK $sdk",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (info.dependencies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    info.dependencies.take(10).forEach { dep ->
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = "${dep.name}: ${dep.version}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    currentPath: String,
    onNavigate: (String) -> Unit
) {
    val segments = if (currentPath.isBlank()) emptyList() else currentPath.split("/")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { onNavigate("") }
        ) {
            Text(
                text = stringResource(R.string.repo_detail_root),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        segments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            val accumulatedPath = segments.take(index + 1).joinToString("/")
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onNavigate(accumulatedPath) }
            ) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentRow(
    item: RepoContentItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.type == ContentType.DIR, onClick = onClick),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (item.type == ContentType.DIR) {
                    Icons.Rounded.Folder
                } else {
                    Icons.AutoMirrored.Rounded.InsertDriveFile
                },
                contentDescription = null,
                tint = if (item.type == ContentType.DIR) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.type == ContentType.FILE) {
                Text(
                    text = DateTimeUtils.formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (item.type == ContentType.DIR) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    title: String,
    subtitle: String,
    progress: Float?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium
        )
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
