package com.flutterforge.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.R
import com.flutterforge.core.common.AppError
import com.flutterforge.core.common.DateTimeUtils
import com.flutterforge.core.common.Resource
import com.flutterforge.core.ui.components.EmptyStateView
import com.flutterforge.core.ui.components.ErrorStateView
import com.flutterforge.core.ui.components.FlutterBadge
import com.flutterforge.core.ui.components.LanguageDot
import com.flutterforge.core.ui.components.PrivateBadge
import com.flutterforge.core.ui.components.ShimmerRepoCard
import com.flutterforge.core.ui.components.toReadableMessage
import com.flutterforge.core.ui.theme.StarAmber
import com.flutterforge.data.paging.AppErrorException
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRepositoryClick: (Repository) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val flutterStatusMap by viewModel.flutterStatusMap.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.repositories.collectAsLazyPagingItems()

    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading &&
        lazyPagingItems.itemCount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    val user = (userState as? Resource.Success)?.data
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (user != null) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.login,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Text(
                            text = user?.name ?: user?.login ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.action_close),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("home_search_field")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RepoFilterChip(
                        label = stringResource(R.string.home_filter_all),
                        selected = selectedFilter == RepoFilter.ALL,
                        onClick = { viewModel.onFilterSelected(RepoFilter.ALL) },
                        tag = "filter_all_chip"
                    )
                    RepoFilterChip(
                        label = stringResource(R.string.home_filter_flutter),
                        selected = selectedFilter == RepoFilter.FLUTTER_ONLY,
                        onClick = { viewModel.onFilterSelected(RepoFilter.FLUTTER_ONLY) },
                        tag = "filter_flutter_chip"
                    )
                    RepoFilterChip(
                        label = stringResource(R.string.home_filter_private),
                        selected = selectedFilter == RepoFilter.PRIVATE_ONLY,
                        onClick = { viewModel.onFilterSelected(RepoFilter.PRIVATE_ONLY) },
                        tag = "filter_private_chip"
                    )
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.onRefresh()
                        lazyPagingItems.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val refreshState = lazyPagingItems.loadState.refresh) {
                        is LoadState.Loading if lazyPagingItems.itemCount == 0 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(6) {
                                    ShimmerRepoCard()
                                }
                            }
                        }

                        is LoadState.Error if lazyPagingItems.itemCount == 0 -> {
                            val appError = (refreshState.error as? AppErrorException)?.appError
                                ?: AppError.Unknown(refreshState.error.localizedMessage)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ErrorStateView(
                                    error = appError,
                                    onRetry = {
                                        viewModel.loadUser()
                                        lazyPagingItems.retry()
                                    }
                                )
                            }
                        }

                        is LoadState.NotLoading if lazyPagingItems.itemCount == 0 -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(
                                    icon = Icons.Rounded.FolderOff,
                                    title = stringResource(R.string.home_empty_title),
                                    subtitle = stringResource(R.string.home_empty_subtitle)
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("repo_lazy_list")
                            ) {
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = lazyPagingItems.itemKey { it.id }
                                ) { index ->
                                    val repo = lazyPagingItems[index]
                                    if (repo != null) {
                                        LaunchedEffect(repo.fullName) {
                                            viewModel.ensureFlutterChecked(repo)
                                        }
                                        val isFlutter = repo.isFlutterProject
                                            ?: flutterStatusMap[repo.fullName]
                                            ?: false

                                        RepositoryRow(
                                            repo = repo,
                                            isFlutter = isFlutter,
                                            onClick = { onRepositoryClick(repo) }
                                        )
                                        if (index < lazyPagingItems.itemCount - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }

                                when (val appendState = lazyPagingItems.loadState.append) {
                                    is LoadState.Loading -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    is LoadState.Error -> {
                                        val appError = (appendState.error as? AppErrorException)?.appError
                                            ?: AppError.Unknown(appendState.error.localizedMessage)
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = appError.toReadableMessage(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                OutlinedButton(onClick = { lazyPagingItems.retry() }) {
                                                    Text(text = stringResource(R.string.action_retry))
                                                }
                                            }
                                        }
                                    }

                                    is LoadState.NotLoading -> Unit
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
private fun RepoFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun RepositoryRow(
    repo: Repository,
    isFlutter: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("repo_card_${repo.name}"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFlutter) {
                        FlutterBadge()
                    }
                    if (repo.isPrivate) {
                        PrivateBadge()
                    }
                }
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    text = repo.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!repo.language.isNullOrBlank()) {
                    LanguageDot(language = repo.language!!)
                }

                if (repo.starsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = StarAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = repo.starsCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = DateTimeUtils.formatRelativeTimePt(repo.updatedAtIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
