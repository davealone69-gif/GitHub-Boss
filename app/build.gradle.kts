// Security-safe Kotlin DSL conversion.
// Release signing is configured only from protected Gradle properties or environment variables.
// Debug builds do not require release credentials.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseStoreFile = providers.gradleProperty("releaseStoreFile")
    .orElse(providers.environmentVariable("RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("releaseStorePassword")
    .orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias")
    .orElse(providers.environmentVariable("RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword")
    .orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD"))

val releaseSigningConfigured = releaseStoreFile.isPresent &&
    releaseStorePassword.isPresent &&
    releaseKeyAlias.isPresent &&
    releaseKeyPassword.isPresent

android {
    namespace = "com.threedouble.d"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.threedouble.d"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.google.filament)
    implementation(libs.google.gltfio)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)
}

// Every release task depends on this guard. No release artifact can be produced unsigned.
val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Verifies that release signing credentials and keystore are available."

    doLast {
        val missing = buildList {
            if (!releaseStoreFile.isPresent) add("RELEASE_STORE_FILE")
            if (!releaseStorePassword.isPresent) add("RELEASE_STORE_PASSWORD")
            if (!releaseKeyAlias.isPresent) add("RELEASE_KEY_ALIAS")
            if (!releaseKeyPassword.isPresent) add("RELEASE_KEY_PASSWORD")
        }
        check(missing.isEmpty()) {
            "Release signing is blocked. Configure protected Gradle properties or environment variables: ${missing.joinToString()}"
        }
        val keystore = file(releaseStoreFile.get())
        check(keystore.isFile) {
            "Release signing is blocked. The configured keystore file does not exist: ${keystore.name}"
        }
    }
}

tasks.configureEach {
    if (name.contains("Release") && name != verifyReleaseSigning.name) {
        dependsOn(verifyReleaseSigning)
    }
}
