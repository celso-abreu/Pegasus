package com.flutterforge.ui.repodetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.Branch
import com.flutterforge.domain.model.DownloadState
import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.RepoContentItem
import com.flutterforge.domain.usecase.GetBranchesUseCase
import com.flutterforge.domain.usecase.GetPubspecPreviewUseCase
import com.flutterforge.domain.usecase.GetRepoContentsUseCase
import com.flutterforge.domain.usecase.ImportProjectUseCase
import com.flutterforge.domain.usecase.ObserveDownloadStateUseCase
import com.flutterforge.ui.navigation.RepoDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoDetailUiState(
    val owner: String = "",
    val repoName: String = "",
    val description: String? = null,
    val isPrivate: Boolean = false,
    val selectedBranch: String = "main",
    val branchesState: Resource<List<Branch>> = Resource.Loading,
    val currentPath: String = "",
    val contentsState: Resource<List<RepoContentItem>> = Resource.Loading,
    val pubspecState: Resource<FlutterProjectInfo?> = Resource.Loading,
    val showApkSoonSheet: Boolean = false
)

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getRepoContentsUseCase: GetRepoContentsUseCase,
    private val getPubspecPreviewUseCase: GetPubspecPreviewUseCase,
    private val importProjectUseCase: ImportProjectUseCase,
    observeDownloadStateUseCase: ObserveDownloadStateUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RepoDetailRoute>()

    private val initialBranch = savedStateHandle.get<String>(KEY_BRANCH) ?: route.defaultBranch
    private val initialPath = savedStateHandle.get<String>(KEY_PATH) ?: ""

    private val _uiState = MutableStateFlow(
        RepoDetailUiState(
            owner = route.owner,
            repoName = route.repo,
            description = route.description,
            isPrivate = route.isPrivate,
            selectedBranch = initialBranch,
            currentPath = initialPath
        )
    )
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    val downloadState: StateFlow<DownloadState> = observeDownloadStateUseCase()

    init {
        loadBranches()
        loadContents(path = initialPath, branch = initialBranch)
        loadPubspec(branch = initialBranch)
    }

    fun loadBranches() {
        viewModelScope.launch {
            _uiState.update { it.copy(branchesState = Resource.Loading) }
            val result = getBranchesUseCase(route.owner, route.repo)
            _uiState.update { it.copy(branchesState = result) }
        }
    }

    fun onBranchSelected(branchName: String) {
        if (branchName == _uiState.value.selectedBranch) return
        savedStateHandle[KEY_BRANCH] = branchName
        savedStateHandle[KEY_PATH] = ""
        _uiState.update {
            it.copy(
                selectedBranch = branchName,
                currentPath = ""
            )
        }
        loadContents(path = "", branch = branchName)
        loadPubspec(branch = branchName)
    }

    fun navigateToDirectory(path: String) {
        savedStateHandle[KEY_PATH] = path
        _uiState.update { it.copy(currentPath = path) }
        loadContents(path = path, branch = _uiState.value.selectedBranch)
    }

    fun navigateUpDirectory(): Boolean {
        val current = _uiState.value.currentPath
        if (current.isBlank()) return false
        val parent = current.substringBeforeLast('/', "")
        navigateToDirectory(parent)
        return true
    }

    fun reloadContents() {
        val state = _uiState.value
        loadContents(path = state.currentPath, branch = state.selectedBranch)
    }

    private fun loadContents(path: String, branch: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentsState = Resource.Loading) }
            val result = getRepoContentsUseCase(
                owner = route.owner,
                repo = route.repo,
                path = path,
                ref = branch
            )
            _uiState.update { it.copy(contentsState = result) }
        }
    }

    private fun loadPubspec(branch: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pubspecState = Resource.Loading) }
            val result = getPubspecPreviewUseCase(
                owner = route.owner,
                repo = route.repo,
                ref = branch
            )
            _uiState.update { it.copy(pubspecState = result) }
        }
    }

    fun startImport() {
        val state = _uiState.value
        importProjectUseCase(
            owner = state.owner,
            repo = state.repoName,
            branch = state.selectedBranch
        )
    }

    fun openApkSoonSheet() {
        _uiState.update { it.copy(showApkSoonSheet = true) }
    }

    fun closeApkSoonSheet() {
        _uiState.update { it.copy(showApkSoonSheet = false) }
    }

    private companion object {
        const val KEY_BRANCH = "detail_selected_branch"
        const val KEY_PATH = "detail_current_path"
    }
}
