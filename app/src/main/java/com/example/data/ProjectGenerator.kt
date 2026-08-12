package com.example.data

data class GeneratedFile(
    val path: String,
    val category: String, // "Android", "Workflow", "Docs", "Config"
    val content: String
)

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

      - name: Grant execute permission for gradlew
        run: |
          if [ -f "./gradlew" ]; then
            chmod +x gradlew
          else
            echo "gradlew not found, using system gradle"
          fi

      - name: Build Debug APK
        run: |
          if [ -f "./gradlew" ]; then
            ./gradlew assembleDebug --no-daemon
          else
            gradle assembleDebug --no-daemon
          fi

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
