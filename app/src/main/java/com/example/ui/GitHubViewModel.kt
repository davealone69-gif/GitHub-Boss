package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: GitHubUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class GitHubViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val gitHubRepo = GitHubRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _reposState = MutableStateFlow<ApiResult<List<GitHubRepo>>>(ApiResult.Success(emptyList()))
    val reposState: StateFlow<ApiResult<List<GitHubRepo>>> = _reposState.asStateFlow()

    private val _issuesState = MutableStateFlow<ApiResult<List<GitHubIssue>>>(ApiResult.Success(emptyList()))
    val issuesState: StateFlow<ApiResult<List<GitHubIssue>>> = _issuesState.asStateFlow()

    private val _workflowRunsState = MutableStateFlow<ApiResult<List<GitHubWorkflowRun>>>(ApiResult.Success(emptyList()))
    val workflowRunsState: StateFlow<ApiResult<List<GitHubWorkflowRun>>> = _workflowRunsState.asStateFlow()

    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    val selectedRepo: StateFlow<GitHubRepo?> = _selectedRepo.asStateFlow()

    private val _repoCreationState = MutableStateFlow<ApiResult<GitHubRepo>?>(null)
    val repoCreationState: StateFlow<ApiResult<GitHubRepo>?> = _repoCreationState.asStateFlow()

    init {
        checkSavedToken()
    }

    fun checkSavedToken() {
        val savedToken = tokenManager.getToken()
        if (!savedToken.isNullOrBlank()) {
            loginWithToken(savedToken)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun loginWithToken(patToken: String) {
        val cleanToken = patToken.trim()
        if (cleanToken.isBlank()) {
            _authState.value = AuthState.Error("Please enter a valid GitHub Personal Access Token")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            when (val result = gitHubRepo.getAuthenticatedUser(cleanToken)) {
                is ApiResult.Success -> {
                    tokenManager.saveToken(cleanToken)
                    _authState.value = AuthState.Authenticated(result.data)
                    refreshData()
                }
                is ApiResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
        _authState.value = AuthState.Unauthenticated
        _reposState.value = ApiResult.Success(emptyList())
        _issuesState.value = ApiResult.Success(emptyList())
        _workflowRunsState.value = ApiResult.Success(emptyList())
        _selectedRepo.value = null
    }

    fun refreshData() {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            // Fetch repositories
            _reposState.value = ApiResult.Loading
            val reposRes = gitHubRepo.getUserRepos(token)
            _reposState.value = reposRes

            if (reposRes is ApiResult.Success && reposRes.data.isNotEmpty()) {
                if (_selectedRepo.value == null) {
                    _selectedRepo.value = reposRes.data.first()
                }
            }

            // Fetch issues
            _issuesState.value = ApiResult.Loading
            _issuesState.value = gitHubRepo.getUserIssues(token)

            // Fetch workflow runs for selected repo if available
            _selectedRepo.value?.let { repo ->
                fetchWorkflowRuns(repo)
            }
        }
    }

    fun selectRepo(repo: GitHubRepo) {
        _selectedRepo.value = repo
        fetchWorkflowRuns(repo)
    }

    fun fetchWorkflowRuns(repo: GitHubRepo) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _workflowRunsState.value = ApiResult.Loading
            _workflowRunsState.value = gitHubRepo.getRepoWorkflowRuns(token, repo.owner?.login ?: repo.fullName.substringBefore("/"), repo.name)
        }
    }

    fun createRepositoryOnGitHub(name: String, description: String?, isPrivate: Boolean) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _repoCreationState.value = ApiResult.Loading
            val result = gitHubRepo.createRepository(token, name, description, isPrivate)
            _repoCreationState.value = result
            if (result is ApiResult.Success) {
                refreshData()
            }
        }
    }

    fun clearRepoCreationState() {
        _repoCreationState.value = null
    }

    fun getSavedToken(): String? = tokenManager.getToken()
}
