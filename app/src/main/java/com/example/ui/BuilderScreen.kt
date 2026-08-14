package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiResult
import com.example.data.GeneratedFile

@Composable
fun BuilderScreen(viewModel: GitHubViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val codeGen by viewModel.codeGenState.collectAsState()
    val hasGemini by viewModel.hasGeminiKey.collectAsState()

    var prompt by remember { mutableStateOf("Note list screen with search and Room") }
    var geminiKeyInput by remember { mutableStateOf("") }
    var useGemini by remember { mutableStateOf(hasGemini) }
    var includeRoom by remember { mutableStateOf(true) }
    var includeHilt by remember { mutableStateOf(false) }
    var includeWorkflows by remember { mutableStateOf(true) }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var showKey by remember { mutableStateOf(false) }

    val files: List<GeneratedFile> = when (val s = codeGen) {
        is ApiResult.Success -> s.data.files
        else -> emptyList()
    }
    val sourceLabel = when (val s = codeGen) {
        is ApiResult.Success -> s.data.source
        else -> null
    }
    val genError = when (val s = codeGen) {
        is ApiResult.Success -> s.data.error
        is ApiResult.Error -> s.message
        else -> null
    }
    val isLoading = codeGen is ApiResult.Loading

    LaunchedEffect(hasGemini) {
        if (hasGemini) useGemini = true
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Text → Kotlin (free)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe the screen or app...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Note list with search",
                        "Login form email password",
                        "Weather with Retrofit",
                        "Todo list Room",
                        "Simple settings screen"
                    ).forEach { p ->
                        SuggestionChip(onClick = { prompt = p }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                // Gemini free key
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = useGemini,
                        onClick = { useGemini = !useGemini },
                        label = { Text(if (hasGemini) "Gemini ON" else "Use Gemini") },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        }
                    )
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Hide key" else "API key")
                    }
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                            )
                        }
                    ) {
                        Text("Get free key")
                    }
                }

                if (showKey) {
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key (free)") },
                        placeholder = { Text("AIza...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            TextButton(onClick = {
                                if (geminiKeyInput.isNotBlank()) {
                                    viewModel.saveGeminiKey(geminiKeyInput)
                                    useGemini = true
                                    Toast.makeText(context, "Gemini key saved", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Save") }
                        }
                    )
                    if (hasGemini) {
                        TextButton(onClick = {
                            viewModel.clearGeminiKey()
                            useGemini = false
                            geminiKeyInput = ""
                        }) { Text("Clear saved key") }
                    }
                    Text(
                        "Free from Google AI Studio. Template always works without a key.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = includeRoom, onClick = { includeRoom = !includeRoom }, label = { Text("Room") })
                    FilterChip(selected = includeHilt, onClick = { includeHilt = !includeHilt }, label = { Text("Hilt") })
                    FilterChip(selected = includeWorkflows, onClick = { includeWorkflows = !includeWorkflows }, label = { Text("CI") })
                }

                Button(
                    onClick = {
                        selectedFileIndex = 0
                        viewModel.generateCode(
                            prompt = prompt,
                            useGemini = useGemini,
                            includeRoom = includeRoom,
                            includeHilt = includeHilt,
                            includeWorkflows = includeWorkflows,
                            includeGeminiStub = false
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading && prompt.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (useGemini && hasGemini) "Generate with Gemini (free)"
                            else "Generate Kotlin (free template)"
                        )
                    }
                }

                sourceLabel?.let {
                    Text(
                        "Source: ${if (it == "gemini") "Gemini" else "Template"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it == "gemini") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                genError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (files.isNotEmpty()) {
            BuilderCodeExplorer(
                files = files,
                selectedIndex = selectedFileIndex.coerceIn(0, files.lastIndex),
                onSelectFile = { selectedFileIndex = it },
                onCopy = {
                    clipboard.setText(AnnotatedString(it))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }
            )
        } else if (!isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Hit Generate — free template always works.\nAdd a free Gemini key for smarter code.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BuilderCodeExplorer(
    files: List<GeneratedFile>,
    selectedIndex: Int,
    onSelectFile: (Int) -> Unit,
    onCopy: (String) -> Unit
) {
    val current = files.getOrNull(selectedIndex)
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
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
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (file.category) {
                                "Workflow" -> Icons.Default.Build
                                "Docs" -> Icons.Default.InsertDriveFile
                                else -> Icons.Default.Code
                            },
                            null,
                            Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            file.path.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        if (current != null) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        current.path,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onCopy(current.content) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    }
                }
                Surface(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
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
