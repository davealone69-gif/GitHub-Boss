# Permanent APK Build Kit

**Why this exists:** Google AI Studio (and most AI code generators) keep breaking Gradle files. They mix old `buildscript {}` style with version catalogs, put wrong AGP/Kotlin combos, and leave random terminal commands as files.

These files are known-good and tested patterns. **Copy them over the broken ones** when AI Studio fucks the project again.

## How to use

1. When build is broken → copy files from this folder into the project root / app/
2. Run `./gradlew assembleDebug --no-daemon`
3. Profit

## Files in this kit

| File | Put it here |
|------|-------------|
| `root-build.gradle.kts` | → `build.gradle.kts` (project root) |
| `app-build.gradle.kts` | → `app/build.gradle.kts` |
| `libs.versions.toml` | → `gradle/libs.versions.toml` |
| `gradle-wrapper.properties` | → `gradle/wrapper/gradle-wrapper.properties` |
| `settings.gradle.kts` | → `settings.gradle.kts` |

## Current known-good combo (Aug 2026)

- Gradle: **8.9**
- AGP: **8.7.3**
- Kotlin: **2.0.21**
- Compose BOM: **2024.10.01**
- compileSdk / targetSdk: **35**
- minSdk: **24**
- Java: **17**

This combination actually builds. Do not let AI “upgrade” it to AGP 9.x + Kotlin 2.2 unless you have verified it yourself.
