package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
                onLogin = { viewModel.loginWithToken(it) }
            )
        }
        is AuthState.Authenticating -> {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Authenticating with GitHub...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is AuthState.Authenticated -> MainAppScreen(user = state.user, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(errorMessage: String?, onLogin: (String) -> Unit) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                Modifier.fillMaxWidth(0.92f).widthIn(max = 500.dp).padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Text("Connect GitHub Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Enter a Personal Access Token (PAT) for repos, issues, Actions, search, and notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("github_pat_input"),
                        label = { Text("Personal Access Token") },
                        placeholder = { Text("ghp_... or github_pat_...") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "Toggle")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Required scopes:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• repo  • workflow  • read:user  • notifications", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { onLogin(tokenInput) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("connect_github_button"),
                        enabled = tokenInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Connect & Verify", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://github.com/settings/tokens/new?scopes=repo,workflow,read:user,notifications&description=GitHub-Boss"
                            ))
                        )
                    }) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate Token on GitHub")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(user: GitHubUser, viewModel: GitHubViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val reposResult by viewModel.reposState.collectAsState()
    val issuesResult by viewModel.issuesState.collectAsState()
    val workflowRunsResult by viewModel.workflowRunsState.collectAsState()
    val notificationsResult by viewModel.notificationsState.collectAsState()
    val searchResult by viewModel.searchState.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()
    val repoCreationState by viewModel.repoCreationState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var promptInput by remember { mutableStateOf("Note list screen with search and Room database") }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }
    var includeRoom by remember { mutableStateOf(true) }
    var includeHilt by remember { mutableStateOf(false) }
    var includeWorkflows by remember { mutableStateOf(true) }
    var includeGemini by remember { mutableStateOf(false) }

    var showCreateRepoDialog by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("") }
    var newRepoDesc by remember { mutableStateOf("") }
    var newRepoPrivate by remember { mutableStateOf(false) }

    val projectFiles = remember(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini) {
        generateProjectFiles(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini, "24")
    }

    val tabs = listOf("Repos", "Search", "Issues", "Actions", "Notifs", "Builder")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!user.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Text(user.name ?: user.login, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${user.login} • ${user.publicRepos} repos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = { viewModel.logout() }, modifier = Modifier.testTag("logout_button")) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (selectedTab) {
                    0 -> RealReposView(
                        reposResult = reposResult,
                        selectedRepo = selectedRepo,
                        onSelectRepo = { viewModel.selectRepo(it) },
                        onCreateRepoClick = { showCreateRepoDialog = true },
                        onStar = { viewModel.starSelectedRepo(); Toast.makeText(context, "Starred", Toast.LENGTH_SHORT).show() },
                        onUnstar = { viewModel.unstarSelectedRepo(); Toast.makeText(context, "Unstarred", Toast.LENGTH_SHORT).show() },
                        onOpenWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    )
                    1 -> SearchReposView(
                        searchResult = searchResult,
                        onSearch = { viewModel.searchRepos(it) },
                        onOpenWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) },
                        onSelectRepo = { viewModel.selectRepo(it); selectedTab = 0 }
                    )
                    2 -> RealIssuesView(
                        issuesResult = issuesResult,
                        onOpenWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    )
                    3 -> RealWorkflowRunsView(
                        selectedRepo = selectedRepo,
                        workflowRunsResult = workflowRunsResult,
                        onRefresh = { selectedRepo?.let { viewModel.fetchWorkflowRuns(it) } },
                        onOpenWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    )
                    4 -> NotificationsView(
                        result = notificationsResult,
                        onRefresh = { viewModel.refreshData() },
                        onOpenWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    )
                    5 -> AppBuilderGeneratorView(
                        promptInput = promptInput,
                        onPromptChange = { promptInput = it },
                        presetPrompts = listOf(
                            "Note list with search",
                            "Login form email password",
                            "Weather app with Retrofit",
                            "Fitness tracker steps",
                            "Simple todo list"
                        ),
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
                            scope.launch {
                                isGenerating = true
                                delay(300)
                                isGenerating = false
                            }
                        },
                        projectFiles = projectFiles,
                        selectedFileIndex = selectedFileIndex,
                        onSelectFile = { selectedFileIndex = it },
                        onCopyFile = {
                            clipboardManager.setText(AnnotatedString(it))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        onCreateRemoteRepo = { name ->
                            viewModel.createRepositoryOnGitHub(name, "Generated: $promptInput", false)
                        }
                    )
                }
            }
        }
    }

    if (showCreateRepoDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRepoDialog = false },
            title = { Text("Create Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(newRepoName, { newRepoName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newRepoDesc, { newRepoDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(newRepoPrivate, { newRepoPrivate = it })
                        Text("Private")
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
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateRepoDialog = false }) { Text("Cancel") } }
        )
    }

    LaunchedEffect(repoCreationState) {
        when (val res = repoCreationState) {
            is ApiResult.Success -> {
                Toast.makeText(context, "Created ${res.data.fullName}", Toast.LENGTH_LONG).show()
                viewModel.clearRepoCreationState()
            }
            is ApiResult.Error -> {
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
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
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    var localFilter by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = localFilter,
                onValueChange = { localFilter = it },
                placeholder = { Text("Filter your repos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onCreateRepoClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(52.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("New")
            }
        }

        if (selectedRepo != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(selectedRepo.name, maxLines = 1) }
                )
                IconButton(onClick = onStar) {
                    Icon(Icons.Default.Star, "Star", tint = Color(0xFFEAB308))
                }
                IconButton(onClick = onUnstar) {
                    Icon(Icons.Outlined.StarBorder, "Unstar")
                }
                IconButton(onClick = { onOpenWeb(selectedRepo.htmlUrl) }) {
                    Icon(Icons.Default.OpenInNew, "Open")
                }
            }
        }

        when (reposResult) {
            is ApiResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("Failed: ${reposResult.message}", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            is ApiResult.Success -> {
                val filtered = reposResult.data.filter {
                    it.name.contains(localFilter, true) || (it.description?.contains(localFilter, true) == true)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.id }) { repo ->
                            RepoCard(
                                repo = repo,
                                isSelected = selectedRepo?.id == repo.id,
                                onClick = { onSelectRepo(repo) },
                                onOpenWeb = { onOpenWeb(repo.htmlUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchReposView(
    searchResult: ApiResult<List<GitHubRepo>>,
    onSearch: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Search GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("e.g. jetpack compose language:kotlin") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch(query.trim())
                    keyboard?.hide()
                })
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onSearch(query.trim())
                    keyboard?.hide()
                },
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Go")
            }
        }
        Text(
            "Tips: language:kotlin  stars:>100  user:davealone69-gif",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (searchResult) {
            is ApiResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(searchResult.message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            is ApiResult.Success -> {
                if (searchResult.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isBlank()) "Type a query and hit Go" else "No results",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text("${searchResult.data.size} results", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(searchResult.data, key = { it.id }) { repo ->
                            RepoCard(
                                repo = repo,
                                isSelected = false,
                                onClick = { onSelectRepo(repo) },
                                onOpenWeb = { onOpenWeb(repo.htmlUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsView(
    result: ApiResult<List<GitHubNotification>>,
    onRefresh: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        when (result) {
            is ApiResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(result.message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            is ApiResult.Success -> {
                if (result.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(result.data, key = { it.id }) { n ->
                            Card(
                                Modifier.fillMaxWidth().clickable {
                                    n.subject?.url?.let { onOpenWeb(it.replace("api.github.com/repos", "github.com").replace("/pulls/", "/pull/")) }
                                        ?: n.repository?.htmlUrl?.let { onOpenWeb(it) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (n.unread) MaterialTheme.colorScheme.primaryContainer.copy(0.35f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            when (n.subject?.type) {
                                                "PullRequest" -> Icons.Default.MergeType
                                                "Issue" -> Icons.Default.BugReport
                                                else -> Icons.Default.Notifications
                                            },
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            n.subject?.title ?: "Notification",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${n.repository?.fullName ?: ""} • ${n.reason ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun RepoCard(
    repo: GitHubRepo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onOpenWeb: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(if (repo.private) Icons.Default.Lock else Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Text(repo.fullName.ifBlank { repo.name }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onOpenWeb) { Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp)) }
            }
            if (!repo.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(repo.description!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                repo.language?.let {
                    Text("• $it", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Color(0xFFEAB308))
                    Text("${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CallSplit, null, Modifier.size(14.dp))
                    Text("${repo.forksCount}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun RealIssuesView(issuesResult: ApiResult<List<GitHubIssue>>, onOpenWeb: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Issues & Pull Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        when (issuesResult) {
            is ApiResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(issuesResult.message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            is ApiResult.Success -> {
                if (issuesResult.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues or PRs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(issuesResult.data, key = { it.id }) { issue ->
                            val isPr = issue.pullRequest != null
                            Card(Modifier.fillMaxWidth().clickable { onOpenWeb(issue.htmlUrl) }, shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                            Icon(
                                                if (isPr) Icons.Default.MergeType else Icons.Default.BugReport,
                                                null,
                                                tint = if (isPr) Color(0xFFA855F7) else Color(0xFF10B981)
                                            )
                                            Text("#${issue.number} ${issue.title}", fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(issue.state.uppercase()) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (issue.state == "open") Color(0xFF10B981).copy(0.2f) else Color.Gray.copy(0.2f)
                                            )
                                        )
                                    }
                                    if (!issue.body.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(issue.body!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("GitHub Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(selectedRepo?.fullName ?: "Select a repo in Repos tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
        }
        if (selectedRepo == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a repository from the Repos tab first.")
            }
            return
        }
        when (workflowRunsResult) {
            is ApiResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Error -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(workflowRunsResult.message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            is ApiResult.Success -> {
                if (workflowRunsResult.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No workflow runs for ${selectedRepo.name}")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(workflowRunsResult.data, key = { it.id }) { run ->
                            Card(Modifier.fillMaxWidth().clickable { onOpenWeb(run.htmlUrl) }, shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(
                                            when (run.conclusion) {
                                                "success" -> Icons.Default.CheckCircle
                                                "failure" -> Icons.Default.Cancel
                                                else -> Icons.Default.PlayCircle
                                            },
                                            null,
                                            tint = when (run.conclusion) {
                                                "success" -> Color(0xFF10B981)
                                                "failure" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Column {
                                            Text("${run.name ?: "Workflow"} #${run.runNumber}", fontWeight = FontWeight.Bold)
                                            Text("Branch: ${run.headBranch ?: "main"} • ${run.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
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
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Text → Kotlin Code Maker", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe the screen or app...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetPrompts.forEach { p ->
                        SuggestionChip(onClick = { onPromptChange(p) }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = includeRoom, onClick = onToggleRoom, label = { Text("Room") })
                    FilterChip(selected = includeHilt, onClick = onToggleHilt, label = { Text("Hilt") })
                    FilterChip(selected = includeWorkflows, onClick = onToggleWorkflows, label = { Text("CI") })
                    FilterChip(selected = includeGemini, onClick = onToggleGemini, label = { Text("Gemini") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGenerate, modifier = Modifier.weight(1f), enabled = !isGenerating, shape = RoundedCornerShape(10.dp)) {
                        Text("Generate Kotlin")
                    }
                    Button(
                        onClick = {
                            val slug = promptInput.lowercase().replace(Regex("[^a-z0-9]"), "-").take(20).trim('-')
                            onCreateRemoteRepo("app-$slug")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Repo")
                    }
                }
            }
        }
        CodeExplorerView(projectFiles, selectedFileIndex, onSelectFile, onCopyFile)
    }
}

@Composable
fun CodeExplorerView(
    files: List<GeneratedFile>,
    selectedIndex: Int,
    onSelectFile: (Int) -> Unit,
    onCopyFile: (String) -> Unit
) {
    val current = files.getOrNull(selectedIndex) ?: files.firstOrNull()
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.width(140.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(files.indices.toList()) { idx ->
                val file = files[idx]
                val selected = idx == selectedIndex
                Surface(
                    onClick = { onSelectFile(idx) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            when (file.category) {
                                "Workflow" -> Icons.Default.Build
                                "Docs" -> Icons.Default.InsertDriveFile
                                else -> Icons.Default.Code
                            },
                            null,
                            Modifier.size(16.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(file.path.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        if (current != null) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(current.path, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onCopyFile(current.content) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(16.dp))
                    }
                }
                Surface(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)), color = MaterialTheme.colorScheme.surface) {
                    SelectionContainer {
                        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                            item {
                                Text(current.content, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
