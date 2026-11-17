package com.flutterforge.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.flutterforge.core.common.Resource
import com.flutterforge.domain.model.RepoFilter
import com.flutterforge.domain.model.Repository
import com.flutterforge.domain.model.User
import com.flutterforge.domain.repository.GitHubRepository
import com.flutterforge.domain.usecase.GetCurrentUserUseCase
import com.flutterforge.domain.usecase.GetReposUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Collections
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getReposUseCase: GetReposUseCase,
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    val selectedFilter: StateFlow<RepoFilter> = savedStateHandle.getStateFlow(
        KEY_FILTER,
        RepoFilter.ALL.name
    ).let { nameFlow ->
        MutableStateFlow(
            runCatching { RepoFilter.valueOf(nameFlow.value) }.getOrDefault(RepoFilter.ALL)
        ).also { state ->
            viewModelScope.launch {
                nameFlow.collect { raw ->
                    state.value = runCatching { RepoFilter.valueOf(raw) }.getOrDefault(RepoFilter.ALL)
                }
            }
        }
    }

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val userState: StateFlow<Resource<User>> = _userState.asStateFlow()

    private val _flutterStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val flutterStatusMap: StateFlow<Map<String, Boolean>> = _flutterStatusMap.asStateFlow()

    private val detectionSemaphore = Semaphore(4)
    private val checkedOrInFlight = Collections.synchronizedSet(mutableSetOf<String>())

    val repositories: Flow<PagingData<Repository>> = combine(
        searchQuery.debounce(350L).map { it.trim() }.distinctUntilChanged(),
        selectedFilter,
        userState.map { (it as? Resource.Success)?.data?.login }.distinctUntilChanged()
    ) { query, filter, login ->
        Triple(query, filter, login)
    }.flatMapLatest { (query, filter, login) ->
        getReposUseCase(query = query, userLogin = login, filter = filter)
    }.cachedIn(viewModelScope)

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _userState.value = Resource.Loading
            _userState.value = getCurrentUserUseCase()
        }
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    fun onFilterSelected(filter: RepoFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun onRefresh() {
        checkedOrInFlight.clear()
        gitHubRepository.clearDetectionCache()
        _flutterStatusMap.value = emptyMap()
        loadUser()
    }

    fun ensureFlutterChecked(repo: Repository) {
        if (repo.isFlutterProject != null) {
            if (_flutterStatusMap.value[repo.fullName] == null) {
                _flutterStatusMap.update { it + (repo.fullName to repo.isFlutterProject) }
            }
            return
        }
        if (!checkedOrInFlight.add(repo.fullName)) return

        viewModelScope.launch {
            val detected = detectionSemaphore.withPermit {
                gitHubRepository.detectFlutterProject(
                    owner = repo.ownerLogin,
                    repo = repo.name,
                    ref = repo.defaultBranch
                )
            }
            if (detected != null) {
                _flutterStatusMap.update { it + (repo.fullName to detected) }
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "home_search_query"
        const val KEY_FILTER = "home_selected_filter"
    }
}
