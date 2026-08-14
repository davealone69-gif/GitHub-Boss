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

/** Result of a code-gen run (template or Gemini). */
data class CodeGenResult(
    val files: List<GeneratedFile>,
    val source: String, // "template" or "gemini"
    val error: String? = null
)

class GitHubViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val gitHubRepo = GitHubRepository()
    private val geminiService = GeminiCodeService()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _reposState = MutableStateFlow<ApiResult<List<GitHubRepo>>>(ApiResult.Success(emptyList()))
    val reposState: StateFlow<ApiResult<List<GitHubRepo>>> = _reposState.asStateFlow()

    private val _issuesState = MutableStateFlow<ApiResult<List<GitHubIssue>>>(ApiResult.Success(emptyList()))
    val issuesState: StateFlow<ApiResult<List<GitHubIssue>>> = _issuesState.asStateFlow()

    private val _workflowRunsState = MutableStateFlow<ApiResult<List<GitHubWorkflowRun>>>(ApiResult.Success(emptyList()))
    val workflowRunsState: StateFlow<ApiResult<List<GitHubWorkflowRun>>> = _workflowRunsState.asStateFlow()

    private val _notificationsState = MutableStateFlow<ApiResult<List<GitHubNotification>>>(ApiResult.Success(emptyList()))
    val notificationsState: StateFlow<ApiResult<List<GitHubNotification>>> = _notificationsState.asStateFlow()

    private val _searchState = MutableStateFlow<ApiResult<List<GitHubRepo>>>(ApiResult.Success(emptyList()))
    val searchState: StateFlow<ApiResult<List<GitHubRepo>>> = _searchState.asStateFlow()

    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    val selectedRepo: StateFlow<GitHubRepo?> = _selectedRepo.asStateFlow()

    private val _repoCreationState = MutableStateFlow<ApiResult<GitHubRepo>?>(null)
    val repoCreationState: StateFlow<ApiResult<GitHubRepo>?> = _repoCreationState.asStateFlow()

    private val _codeGenState = MutableStateFlow<ApiResult<CodeGenResult>?>(null)
    val codeGenState: StateFlow<ApiResult<CodeGenResult>?> = _codeGenState.asStateFlow()

    private val _hasGeminiKey = MutableStateFlow(tokenManager.hasGeminiKey())
    val hasGeminiKey: StateFlow<Boolean> = _hasGeminiKey.asStateFlow()

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
        _notificationsState.value = ApiResult.Success(emptyList())
        _searchState.value = ApiResult.Success(emptyList())
        _selectedRepo.value = null
    }

    fun saveGeminiKey(key: String) {
        tokenManager.saveGeminiKey(key)
        _hasGeminiKey.value = tokenManager.hasGeminiKey()
    }

    fun clearGeminiKey() {
        tokenManager.clearGeminiKey()
        _hasGeminiKey.value = false
    }

    fun refreshData() {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _reposState.value = ApiResult.Loading
            val reposRes = gitHubRepo.getUserRepos(token)
            _reposState.value = reposRes

            if (reposRes is ApiResult.Success && reposRes.data.isNotEmpty() && _selectedRepo.value == null) {
                _selectedRepo.value = reposRes.data.first()
            }

            _issuesState.value = ApiResult.Loading
            _issuesState.value = gitHubRepo.getUserIssues(token)

            _notificationsState.value = ApiResult.Loading
            _notificationsState.value = gitHubRepo.getNotifications(token)

            _selectedRepo.value?.let { fetchWorkflowRuns(it) }
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
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            _workflowRunsState.value = gitHubRepo.getRepoWorkflowRuns(token, owner, repo.name)
        }
    }

    fun createRepositoryOnGitHub(name: String, description: String?, isPrivate: Boolean) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _repoCreationState.value = ApiResult.Loading
            val result = gitHubRepo.createRepository(token, name, description, isPrivate)
            _repoCreationState.value = result
            if (result is ApiResult.Success) refreshData()
        }
    }

    fun clearRepoCreationState() {
        _repoCreationState.value = null
    }

    fun starSelectedRepo() {
        val token = tokenManager.getToken() ?: return
        val repo = _selectedRepo.value ?: return
        val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
        viewModelScope.launch {
            gitHubRepo.starRepo(token, owner, repo.name)
        }
    }

    fun unstarSelectedRepo() {
        val token = tokenManager.getToken() ?: return
        val repo = _selectedRepo.value ?: return
        val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
        viewModelScope.launch {
            gitHubRepo.unstarRepo(token, owner, repo.name)
        }
    }

    fun searchRepos(query: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _searchState.value = ApiResult.Loading
            _searchState.value = gitHubRepo.searchRepositories(token, query)
        }
    }

    /**
     * Generate Kotlin code.
     * - If [useGemini] and a free Gemini key exists → call Gemini, fall back to template on error
     * - Else → instant free template maker
     */
    fun generateCode(
        prompt: String,
        useGemini: Boolean,
        includeRoom: Boolean,
        includeHilt: Boolean,
        includeWorkflows: Boolean,
        includeGeminiStub: Boolean,
        minSdk: String = "24"
    ) {
        viewModelScope.launch {
            _codeGenState.value = ApiResult.Loading

            val geminiKey = tokenManager.getGeminiKey()
            if (useGemini && !geminiKey.isNullOrBlank()) {
                when (val res = geminiService.generateKotlinCode(geminiKey, prompt)) {
                    is ApiResult.Success -> {
                        val kotlinFiles = GeminiCodeService.parseGeminiOutput(res.data)
                        val generated = kotlinFiles.map {
                            GeneratedFile(
                                path = "app/src/main/java/com/example/${it.path}",
                                category = if (it.name.endsWith(".md")) "Docs" else "Kotlin",
                                content = it.content
                            )
                        }
                        // Still attach project scaffolding from template
                        val scaffold = generateProjectFiles(
                            prompt, includeRoom, includeHilt, includeWorkflows, includeGeminiStub, minSdk
                        ).filter { it.category != "Kotlin" }
                        _codeGenState.value = ApiResult.Success(
                            CodeGenResult(
                                files = generated + scaffold,
                                source = "gemini"
                            )
                        )
                        return@launch
                    }
                    is ApiResult.Error -> {
                        // Fall through to template with error note
                        val template = generateProjectFiles(
                            prompt, includeRoom, includeHilt, includeWorkflows, includeGeminiStub, minSdk
                        )
                        _codeGenState.value = ApiResult.Success(
                            CodeGenResult(
                                files = template,
                                source = "template",
                                error = "Gemini failed: ${res.message}. Used free template instead."
                            )
                        )
                        return@launch
                    }
                    is ApiResult.Loading -> {}
                }
            }

            // Free template path (always works, no key needed)
            val template = generateProjectFiles(
                prompt, includeRoom, includeHilt, includeWorkflows, includeGeminiStub, minSdk
            )
            _codeGenState.value = ApiResult.Success(
                CodeGenResult(files = template, source = "template")
            )
        }
    }

    fun clearCodeGenState() {
        _codeGenState.value = null
    }

    fun getSavedToken(): String? = tokenManager.getToken()
}
