package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.AuthState
import com.example.ui.GitHubViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: GitHubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppBuilderApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppBuilderApp(viewModel: GitHubViewModel) {
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Unauthenticated, is AuthState.Error -> {
            LoginScreen(
                errorMessage = (state as? AuthState.Error)?.message,
                onLogin = { token -> viewModel.loginWithToken(token) }
            )
        }
        is AuthState.Authenticating -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Authenticating with GitHub REST API v3...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is AuthState.Authenticated -> {
            MainAppScreen(
                user = state.user,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    errorMessage: String?,
    onLogin: (String) -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 500.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo Header
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "Connect GitHub Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Enter your GitHub Personal Access Token (PAT) to fetch real repositories, issues, pull requests, and workflow runs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    // Error Box if any
                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // PAT Token Input Field
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_pat_input"),
                        label = { Text("Personal Access Token (PAT)") },
                        placeholder = { Text("ghp_xxxxxxxxxxxx or github_pat_xxxx") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle Token Visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Help Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Required Scopes:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• repo (Full control of private repositories)\n• workflow (Update GitHub Action workflows)\n• read:user (Read user profile data)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onLogin(tokenInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("connect_github_button"),
                        enabled = tokenInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect & Verify Token", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/tokens/new?scopes=repo,workflow,read:user&description=Android%20App%20Builder"))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Token on GitHub.com", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    user: GitHubUser,
    viewModel: GitHubViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val reposResult by viewModel.reposState.collectAsState()
    val issuesResult by viewModel.issuesState.collectAsState()
    val workflowRunsResult by viewModel.workflowRunsState.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()
    val repoCreationState by viewModel.repoCreationState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var promptInput by remember { mutableStateOf("Build a modern Note Taking app with Room database, Material 3 dark theme, and search feature.") }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }

    // Config options
    var includeRoom by remember { mutableStateOf(true) }
    var includeHilt by remember { mutableStateOf(true) }
    var includeWorkflows by remember { mutableStateOf(true) }
    var includeGemini by remember { mutableStateOf(false) }
    var targetMinSdk by remember { mutableStateOf("24") }

    // Dialog state for creating new remote repo
    var showCreateRepoDialog by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("") }
    var newRepoDesc by remember { mutableStateOf("") }
    var newRepoPrivate by remember { mutableStateOf(false) }

    val presetPrompts = listOf(
        "Note Taking App with Room & M3",
        "Fitness & Step Tracker with Charts",
        "Weather Forecast App with Retrofit",
        "AI Assistant with Gemini API",
        "E-Commerce Showcase & Cart"
    )

    val projectFiles = remember(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini, targetMinSdk) {
        generateProjectFiles(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini, targetMinSdk)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!user.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.login.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = user.name ?: user.login,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${user.login} • ${user.publicRepos} repos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Repos") },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.testTag("tab_repos")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Issues/PRs") },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    modifier = Modifier.testTag("tab_issues")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Actions") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    modifier = Modifier.testTag("tab_actions")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("App Builder") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.testTag("tab_app_builder")
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> RealReposView(
                        reposResult = reposResult,
                        selectedRepo = selectedRepo,
                        onSelectRepo = { repo -> viewModel.selectRepo(repo) },
                        onCreateRepoClick = { showCreateRepoDialog = true },
                        onOpenWeb = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    1 -> RealIssuesView(
                        issuesResult = issuesResult,
                        onOpenWeb = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    2 -> RealWorkflowRunsView(
                        selectedRepo = selectedRepo,
                        workflowRunsResult = workflowRunsResult,
                        onRefresh = { selectedRepo?.let { viewModel.fetchWorkflowRuns(it) } },
                        onOpenWeb = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    3 -> AppBuilderGeneratorView(
                        promptInput = promptInput,
                        onPromptChange = { promptInput = it },
                        presetPrompts = presetPrompts,
                        includeRoom = includeRoom,
                        onToggleRoom = { includeRoom = !includeRoom },
                        includeHilt = includeHilt,
                        onToggleHilt = { includeHilt = !includeHilt },
                        includeWorkflows = includeWorkflows,
                        onToggleWorkflows = { includeWorkflows = !includeWorkflows },
                        includeGemini = includeGemini,
                        onToggleGemini = { includeGemini = !includeGemini },
                        isGenerating = isGenerating,
                        onGenerate = {
                            coroutineScope.launch {
                                isGenerating = true
                                delay(400)
                                isGenerating = false
                            }
                        },
                        projectFiles = projectFiles,
                        selectedFileIndex = selectedFileIndex,
                        onSelectFile = { selectedFileIndex = it },
                        onCopyFile = { content ->
                            clipboardManager.setText(AnnotatedString(content))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onCreateRemoteRepo = { repoName ->
                            viewModel.createRepositoryOnGitHub(
                                name = repoName,
                                description = "Generated from Android App Builder: $promptInput",
                                isPrivate = false
                            )
                        }
                    )
                }
            }
        }
    }

    // Create Remote Repo Dialog
    if (showCreateRepoDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRepoDialog = false },
            title = { Text("Create New GitHub Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newRepoName,
                        onValueChange = { newRepoName = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRepoDesc,
                        onValueChange = { newRepoDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = newRepoPrivate,
                            onCheckedChange = { newRepoPrivate = it }
                        )
                        Text("Private Repository")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRepoName.isNotBlank()) {
                            viewModel.createRepositoryOnGitHub(newRepoName, newRepoDesc, newRepoPrivate)
                            showCreateRepoDialog = false
                            newRepoName = ""
                            newRepoDesc = ""
                        }
                    },
                    enabled = newRepoName.isNotBlank()
                ) {
                    Text("Create on GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Observe creation response
    LaunchedEffect(repoCreationState) {
        when (val res = repoCreationState) {
            is ApiResult.Success -> {
                Toast.makeText(context, "Repository '${res.data.fullName}' created on GitHub!", Toast.LENGTH_LONG).show()
                viewModel.clearRepoCreationState()
            }
            is ApiResult.Error -> {
                Toast.makeText(context, "Error: ${res.message}", Toast.LENGTH_LONG).show()
                viewModel.clearRepoCreationState()
            }
            else -> {}
        }
    }
}

@Composable
fun RealReposView(
    reposResult: ApiResult<List<GitHubRepo>>,
    selectedRepo: GitHubRepo?,
    onSelectRepo: (GitHubRepo) -> Unit,
    onCreateRepoClick: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search repositories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCreateRepoClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New")
            }
        }

        when (reposResult) {
            is ApiResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ApiResult.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Failed to load repos: ${reposResult.message}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is ApiResult.Success -> {
                val filteredRepos = reposResult.data.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            (it.description?.contains(searchQuery, ignoreCase = true) == true)
                }

                if (filteredRepos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No repositories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRepos) { repo ->
                            val isSelected = selectedRepo?.id == repo.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectRepo(repo) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (repo.private) Icons.Default.Lock else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = repo.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(onClick = { onOpenWeb(repo.htmlUrl) }) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Open in Web",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (!repo.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = repo.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repo.language?.let { lang ->
                                            Text(
                                                text = "• $lang",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEAB308))
                                            Text(text = "${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Text(text = "${repo.forksCount}", style = MaterialTheme.typography.labelSmall)
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
}

@Composable
fun RealIssuesView(
    issuesResult: ApiResult<List<GitHubIssue>>,
    onOpenWeb: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "User Issues & Pull Requests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        when (issuesResult) {
            is ApiResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ApiResult.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "Failed to load issues: ${issuesResult.message}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is ApiResult.Success -> {
                if (issuesResult.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No open issues or pull requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(issuesResult.data) { issue ->
                            val isPr = issue.pullRequest != null

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenWeb(issue.htmlUrl) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPr) Icons.Default.MergeType else Icons.Default.BugReport,
                                                contentDescription = null,
                                                tint = if (isPr) Color(0xFFA855F7) else Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "#${issue.number} ${issue.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }

                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(issue.state.uppercase()) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (issue.state == "open") Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                            )
                                        )
                                    }

                                    if (!issue.body.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = issue.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
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
fun RealWorkflowRunsView(
    selectedRepo: GitHubRepo?,
    workflowRunsResult: ApiResult<List<GitHubWorkflowRun>>,
    onRefresh: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GitHub Actions Workflow Runs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedRepo?.fullName ?: "Select a repository in Repos tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Workflow Runs")
            }
        }

        if (selectedRepo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a repository from the Repos tab to view Actions runs.")
            }
            return
        }

        when (workflowRunsResult) {
            is ApiResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ApiResult.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "Failed to load runs: ${workflowRunsResult.message}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is ApiResult.Success -> {
                if (workflowRunsResult.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No GitHub Actions workflow runs found for ${selectedRepo.name}.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(workflowRunsResult.data) { run ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenWeb(run.htmlUrl) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (run.conclusion) {
                                                "success" -> Icons.Default.CheckCircle
                                                "failure" -> Icons.Default.Cancel
                                                else -> Icons.Default.PlayCircle
                                            },
                                            contentDescription = null,
                                            tint = when (run.conclusion) {
                                                "success" -> Color(0xFF10B981)
                                                "failure" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Column {
                                            Text(
                                                text = "${run.name ?: "Workflow"} #${run.runNumber}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Branch: ${run.headBranch ?: "main"} • Status: ${run.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
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
fun AppBuilderGeneratorView(
    promptInput: String,
    onPromptChange: (String) -> Unit,
    presetPrompts: List<String>,
    includeRoom: Boolean,
    onToggleRoom: () -> Unit,
    includeHilt: Boolean,
    onToggleHilt: () -> Unit,
    includeWorkflows: Boolean,
    onToggleWorkflows: () -> Unit,
    includeGemini: Boolean,
    onToggleGemini: () -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    projectFiles: List<GeneratedFile>,
    selectedFileIndex: Int,
    onSelectFile: (Int) -> Unit,
    onCopyFile: (String) -> Unit,
    onCreateRemoteRepo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "App Generator Concept",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe the Android app to generate...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetPrompts.forEach { preset ->
                        SuggestionChip(
                            onClick = { onPromptChange(preset) },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(selected = includeRoom, onClick = onToggleRoom, label = { Text("Room DB") })
                    FilterChip(selected = includeHilt, onClick = onToggleHilt, label = { Text("Hilt DI") })
                    FilterChip(selected = includeWorkflows, onClick = onToggleWorkflows, label = { Text("Workflows") })
                    FilterChip(selected = includeGemini, onClick = onToggleGemini, label = { Text("Gemini AI") })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Re-generate Files")
                    }

                    Button(
                        onClick = {
                            val slug = promptInput.lowercase().replace(Regex("[^a-z0-9]"), "-").take(20).trim('-')
                            onCreateRemoteRepo("app-$slug")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Repo on GitHub")
                    }
                }
            }
        }

        // Code Inspector split view
        CodeExplorerView(
            files = projectFiles,
            selectedIndex = selectedFileIndex,
            onSelectFile = onSelectFile,
            onCopyFile = onCopyFile
        )
    }
}

@Composable
fun CodeExplorerView(
    files: List<GeneratedFile>,
    selectedIndex: Int,
    onSelectFile: (Int) -> Unit,
    onCopyFile: (String) -> Unit
) {
    val currentFile = files.getOrNull(selectedIndex) ?: files.firstOrNull()

    Row(modifier = Modifier.fillMaxSize()) {
        // File Tree Sidebar
        LazyColumn(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(files.indices.toList()) { idx ->
                val file = files[idx]
                val isSelected = idx == selectedIndex
                val fileName = file.path.substringAfterLast("/")

                Surface(
                    onClick = { onSelectFile(idx) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (file.category) {
                                "Workflow" -> Icons.Default.Build
                                "Docs" -> Icons.Default.InsertDriveFile
                                else -> Icons.Default.Code
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Code Display Area
        if (currentFile != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // File Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentFile.path,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onCopyFile(currentFile.content) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy File",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Code Content
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = currentFile.content,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
