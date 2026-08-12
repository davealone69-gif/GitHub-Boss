plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.githubboss" // ← replace with your real package
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.githubboss" // ← replace with your real package
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Jetpack Compose Material 3
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")

    // Lifecycle + ViewModel + Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp (required for OllamaClient)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Android core
    implementation("androidx.core:core-ktx:1.12.0")

    // Optional: JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}