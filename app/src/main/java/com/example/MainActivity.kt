package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppBuilderMainScreen()
            }
        }
    }
}

data class GeneratedFile(
    val path: String,
    val category: String, // "Android", "Workflow", "Docs", "Config"
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBuilderMainScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var promptInput by remember { mutableStateOf("Build a modern Note Taking app with Room database, Material 3 dark theme, and search feature.") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }

    // Configuration states
    var includeRoom by remember { mutableStateOf(true) }
    var includeHilt by remember { mutableStateOf(true) }
    var includeWorkflows by remember { mutableStateOf(true) }
    var includeGemini by remember { mutableStateOf(false) }
    var targetMinSdk by remember { mutableStateOf("24") }

    // Simulation states
    var isSimulatingCi by remember { mutableStateOf(false) }
    var ciLogs by remember { mutableStateOf(listOf<String>()) }
    var ciProgress by remember { mutableFloatStateOf(0f) }

    val presetPrompts = listOf(
        "Note Taking App with Room & M3",
        "Fitness & Step Tracker with Charts",
        "Weather Forecast App with Retrofit",
        "AI Assistant with Gemini API",
        "E-Commerce Showcase & Cart"
    )

    // Generate files based on current prompt & config
    val projectFiles = remember(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini, targetMinSdk) {
        generateProjectFiles(promptInput, includeRoom, includeHilt, includeWorkflows, includeGemini, targetMinSdk)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Android App Builder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jetpack Compose & GitHub Actions Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(projectFiles.joinToString("\n\n") { "--- ${it.path} ---\n${it.content}" }))
                            Toast.makeText(context, "Full project copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_all_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Full Project")
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
            // Prompt Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "App Concept / Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_input_field"),
                        placeholder = { Text("Describe the app you want to build...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetPrompts.forEach { preset ->
                            SuggestionChip(
                                onClick = { promptInput = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("preset_chip_${preset.take(6).lowercase()}")
                            )
                        }
                    }

                    // Configuration Toggles Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = includeRoom,
                            onClick = { includeRoom = !includeRoom },
                            label = { Text("Room Database") },
                            leadingIcon = { if (includeRoom) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = includeHilt,
                            onClick = { includeHilt = !includeHilt },
                            label = { Text("Hilt DI") },
                            leadingIcon = { if (includeHilt) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = includeWorkflows,
                            onClick = { includeWorkflows = !includeWorkflows },
                            label = { Text("GitHub Workflows") },
                            leadingIcon = { if (includeWorkflows) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = includeGemini,
                            onClick = { includeGemini = !includeGemini },
                            label = { Text("Gemini AI API") },
                            leadingIcon = { if (includeGemini) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isGenerating = true
                                delay(600)
                                isGenerating = false
                                Toast.makeText(context, "Project files re-generated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_app_button"),
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Project...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Build Android & GitHub Repo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Files & Code (${projectFiles.size})") },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.testTag("tab_files")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Workflows & CI") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    modifier = Modifier.testTag("tab_workflows")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("CI Simulator") },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    modifier = Modifier.testTag("tab_ci_simulator")
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> CodeExplorerView(
                        files = projectFiles,
                        selectedIndex = selectedFileIndex,
                        onSelectFile = { selectedFileIndex = it },
                        onCopyFile = { content ->
                            clipboardManager.setText(AnnotatedString(content))
                            Toast.makeText(context, "File copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> WorkflowGeneratorView(
                        files = projectFiles.filter { it.category == "Workflow" },
                        onCopy = { content ->
                            clipboardManager.setText(AnnotatedString(content))
                            Toast.makeText(context, "Workflow copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> CiSimulatorView(
                        isRunning = isSimulatingCi,
                        progress = ciProgress,
                        logs = ciLogs,
                        onStartSimulation = {
                            coroutineScope.launch {
                                isSimulatingCi = true
                                ciLogs = emptyList()
                                ciProgress = 0f

                                val steps = listOf(
                                    "Setting up JDK 17 environment...",
                                    "Validating Gradle configuration files...",
                                    "Running Detekt static analysis & Android Lint...",
                                    "Executing Robolectric unit test suites...",
                                    "Compiling Jetpack Compose UI components...",
                                    "Building release APK & AAB artifacts...",
                                    "Generating CHANGELOG.md & release tagging...",
                                    "SUCCESS: Android Studio project & GitHub release verified!"
                                )

                                steps.forEachIndexed { idx, log ->
                                    ciLogs = ciLogs + log
                                    ciProgress = (idx + 1).toFloat() / steps.size
                                    delay(500)
                                }

                                isSimulatingCi = false
                            }
                        }
                    )
                }
            }
        }
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

@Composable
fun WorkflowGeneratorView(
    files: List<GeneratedFile>,
    onCopy: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Hardened GitHub Actions Workflows",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Production-ready CI/CD pipelines generated for your repository.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(files) { workflow ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = workflow.path,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { onCopy(workflow.content) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Workflow")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = workflow.content.take(300) + "\n...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CiSimulatorView(
    isRunning: Boolean,
    progress: Float,
    logs: List<String>,
    onStartSimulation: () -> Unit
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
                    text = "CI/CD Pipeline Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verify Gradle build, tests, and APK artifact generation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onStartSimulation,
                enabled = !isRunning,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run CI")
            }
        }

        if (isRunning || progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        // Terminal Log Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (log.startsWith("SUCCESS")) "✓" else ">",
                            color = if (log.startsWith("SUCCESS")) Color(0xFF10B981) else Color(0xFF6366F1),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log,
                            color = Color(0xFFF8FAFC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "Tap 'Run CI' to trigger simulated build and test execution...",
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// File generator logic
fun generateProjectFiles(
    prompt: String,
    includeRoom: Boolean,
    includeHilt: Boolean,
    includeWorkflows: Boolean,
    includeGemini: Boolean,
    minSdk: String
): List<GeneratedFile> {
    val appSlug = prompt.lowercase().replace(Regex("[^a-z0-9]"), "").take(12).ifEmpty { "myapp" }
    
    val list = mutableListOf<GeneratedFile>()

    // MainActivity
    list.add(
        GeneratedFile(
            path = "app/src/main/java/com/example/MainActivity.kt",
            category = "Android",
            content = """
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Generated App: $prompt",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Action */ }) {
            Text("Interactive Action")
        }
    }
}
            """.trimIndent()
        )
    )

    // build.gradle.kts
    list.add(
        GeneratedFile(
            path = "app/build.gradle.kts",
            category = "Android",
            content = """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    ${if (includeHilt) "id(\"com.google.dagger.hilt.android\")" else ""}
    ${if (includeRoom) "alias(libs.plugins.google.devtools.ksp)" else ""}
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.$appSlug.app"
        minSdk = $minSdk
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    ${if (includeRoom) "implementation(libs.androidx.room.runtime)\n    implementation(libs.androidx.room.ktx)" else ""}
    ${if (includeGemini) "implementation(libs.firebase.ai)" else ""}
}
            """.trimIndent()
        )
    )

    // AndroidManifest.xml
    list.add(
        GeneratedFile(
            path = "app/src/main/AndroidManifest.xml",
            category = "Android",
            content = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
            """.trimIndent()
        )
    )

    // Workflows
    if (includeWorkflows) {
        list.add(
            GeneratedFile(
                path = ".github/workflows/build-android.yml",
                category = "Workflow",
                content = """
name: Build & Test Android

on:
  push:
    branches: [ "main", "develop" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradle
        run: chmod +x gradlew || true

      - name: Build Debug APK
        run: gradle assembleDebug --no-daemon

      - name: Run Unit Tests
        run: gradle testDebugUnitTest --no-daemon

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
                """.trimIndent()
            )
        )

        list.add(
            GeneratedFile(
                path = ".github/workflows/release-autotag.yml",
                category = "Workflow",
                content = """
name: Auto Release & Tagging

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
      - name: Build Release Bundle
        run: gradle assembleRelease --no-daemon
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/*.apk
          generate_release_notes: true
                """.trimIndent()
            )
        )
    }

    // Docs
    list.add(
        GeneratedFile(
            path = "README.md",
            category = "Docs",
            content = """
# $prompt

A modern Jetpack Compose Android application built with Material Design 3.

## Features
- **UI Architecture**: Jetpack Compose, Material 3, ViewModel & StateFlow.
- **Local Persistence**: ${if (includeRoom) "Room Database with Kotlin Coroutines Flow." else "In-memory state persistence."}
- **Dependency Injection**: ${if (includeHilt) "Hilt Dependency Injection." else "Standard Constructor Injection."}
- **CI/CD**: Fully automated GitHub Actions workflows for building, testing, and release tagging.

## Setup & Build
1. Clone the repository.
2. Open in Android Studio Hedgehog or newer.
3. Sync Gradle and run on device/emulator.
            """.trimIndent()
        )
    )

    list.add(
        GeneratedFile(
            path = "CHANGELOG.md",
            category = "Docs",
            content = """
# Changelog

## [1.0.0] - ${java.time.LocalDate.now()}
### Added
- Initial project template generation for "$prompt".
- Jetpack Compose M3 UI baseline.
- Automated GitHub Actions CI workflow pipeline.
            """.trimIndent()
        )
    )

    return list
}

