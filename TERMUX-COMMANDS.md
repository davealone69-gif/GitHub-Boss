# Termux Commands — Build APKs on your phone

These are the main commands that actually work for building Android projects from Termux.

## 1. One-time setup (run once)

```bash
pkg update -y && pkg upgrade -y
pkg install -y openjdk-17 git wget curl unzip

# Optional but recommended
pkg install -y android-tools
```

## 2. Get the project

```bash
cd ~
git clone https://github.com/davealone69-gif/GitHub-Boss.git
cd GitHub-Boss
```

## 3. Make gradlew executable + first build

```bash
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```

APK lands at:
`app/build/outputs/apk/debug/app-debug.apk`

## 4. Useful everyday commands

```bash
# Clean + rebuild
./gradlew clean assembleDebug --no-daemon

# Just compile (faster check)
./gradlew :app:compileDebugKotlin --no-daemon

# Install on connected device / emulator
./gradlew installDebug --no-daemon

# Full release build (needs signing config)
./gradlew assembleRelease --no-daemon
```

## 5. When Gradle is fucked again

```bash
# Kill any stuck daemons
./gradlew --stop

# Clear caches (nuclear option)
rm -rf ~/.gradle/caches/
rm -rf .gradle/
./gradlew clean assembleDebug --no-daemon --refresh-dependencies
```

## 6. Quick check if Java is correct

```bash
java -version   # should show 17.x
echo $JAVA_HOME
```

## 7. Copy APK to shared storage (so you can install it)

```bash
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/GitHub-Boss-debug.apk
termux-open /sdcard/Download/GitHub-Boss-debug.apk   # if termux-api is installed
```

---

**Tip:** Keep this file. AI Studio keeps rewriting Gradle files and breaking builds. Use the permanent kit in `/apk-build-kit/` instead of letting it touch the real build files.
