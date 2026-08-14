package com.example.data

/**
 * Text → Kotlin code maker.
 *
 * Takes a natural language description and produces usable Kotlin / Jetpack Compose files.
 * This is intentionally rule + template based so it works offline and stays predictable.
 * For maximum quality you can later pipe the same prompt through a local LLM (Ollama / Dolphin).
 */
object KotlinCodeMaker {

    data class KotlinFile(
        val path: String,
        val name: String,
        val content: String
    )

    fun generate(prompt: String, packageName: String = "com.example"): List<KotlinFile> {
        val clean = prompt.trim().ifBlank { "Simple demo screen" }
        val slug = clean.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(4)
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { "Demo" }

        val className = if (slug.endsWith("Screen") || slug.endsWith("View")) slug else "${slug}Screen"
        val viewModelName = className.removeSuffix("Screen") + "ViewModel"
        val stateName = className.removeSuffix("Screen") + "UiState"

        val wantsList = clean.contains("list", true) || clean.contains("feed", true) || clean.contains("items", true)
        val wantsForm = clean.contains("form", true) || clean.contains("input", true) || clean.contains("login", true)
        val wantsApi = clean.contains("api", true) || clean.contains("network", true) || clean.contains("retrofit", true)
        val wantsRoom = clean.contains("room", true) || clean.contains("database", true) || clean.contains("local", true)

        val files = mutableListOf<KotlinFile>()

        // 1. UiState
        files += KotlinFile(
            path = "ui/$stateName.kt",
            name = "$stateName.kt",
            content = """
package $packageName.ui

data class $stateName(
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "$clean",
    ${if (wantsList) "val items: List<String> = emptyList()," else ""}
    ${if (wantsForm) "val inputText: String = \"\"," else ""}
    val message: String = "Ready"
)
            """.trimIndent()
        )

        // 2. ViewModel
        files += KotlinFile(
            path = "ui/$viewModelName.kt",
            name = "$viewModelName.kt",
            content = """
package $packageName.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class $viewModelName : ViewModel() {

    private val _uiState = MutableStateFlow($stateName())
    val uiState: StateFlow<$stateName> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // TODO: real data source
                kotlinx.coroutines.delay(400)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Loaded successfully",
                        ${if (wantsList) "items = listOf(\"Item 1\", \"Item 2\", \"Item 3\")," else ""}
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    ${if (wantsForm) """
    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun submit() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Input cannot be empty") }
            return
        }
        _uiState.update { it.copy(message = "Submitted: \$text", error = null) }
    }
    """.trimIndent() else ""}
}
            """.trimIndent()
        )

        // 3. Compose Screen
        files += KotlinFile(
            path = "ui/$className.kt",
            name = "$className.kt",
            content = buildScreen(packageName, className, viewModelName, stateName, wantsList, wantsForm)
        )

        // 4. Optional data layer stubs
        if (wantsApi) {
            files += KotlinFile(
                path = "data/ApiService.kt",
                name = "ApiService.kt",
                content = """
package $packageName.data

import retrofit2.http.GET

interface ApiService {
    @GET("endpoint")
    suspend fun fetchData(): List<String>
}
                """.trimIndent()
            )
        }

        if (wantsRoom) {
            files += KotlinFile(
                path = "data/AppEntity.kt",
                name = "AppEntity.kt",
                content = """
package $packageName.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class AppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
                """.trimIndent()
            )
        }

        // 5. Quick usage snippet
        files += KotlinFile(
            path = "USAGE.md",
            name = "USAGE.md",
            content = """
# Generated from: "$clean"

## Files
${files.filter { it.name.endsWith(".kt") }.joinToString("\n") { "- `${it.path}`" }}

## How to use
1. Copy the `.kt` files into your Android project under the matching package.
2. In your Activity / NavHost:

```kotlin
val vm: $viewModelName = viewModel()
$className(viewModel = vm)
```

3. Call `vm.refresh()` when the screen appears if you want auto-load.

This generator is offline and deterministic. For smarter code, feed the same prompt to a local LLM (Ollama / Dolphin) and paste the result here.
            """.trimIndent()
        )

        return files
    }

    private fun buildScreen(
        packageName: String,
        className: String,
        viewModelName: String,
        stateName: String,
        wantsList: Boolean,
        wantsForm: Boolean
    ): String {
        return """
package $packageName.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun $className(
    viewModel: $viewModelName,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.title) })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.error?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }

            Text(text = state.message, style = MaterialTheme.typography.bodyLarge)

            ${if (wantsForm) """
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChange,
                label = { Text("Input") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
            """.trimIndent() else ""}

            ${if (wantsList) """
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.items) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            """.trimIndent() else ""}

            Button(
                onClick = viewModel::refresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }
        }
    }
}
        """.trimIndent()
    }
}
