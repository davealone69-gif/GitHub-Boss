# Termux Commands — Build APKs on your phone

## 1. One-time setup (run once)

```bash
pkg update -y && pkg upgrade -y
pkg install -y openjdk-17 git wget curl unzip
# Optional
pkg install -y android-tools
```

## 2. Get the project

```bash
cd ~
git clone https://github.com/davealone69-gif/GitHub-Boss.git
cd GitHub-Boss
```

## 3. Fix corrupt gradle-wrapper.jar (do this if build says "Invalid or corrupt jarfile")

Git/AI tools sometimes break the binary jar. Regenerate it:

```bash
# Install Gradle once (or use any existing Gradle 8.9+)
pkg install -y wget unzip
cd /tmp
wget -q https://services.gradle.org/distributions/gradle-8.9-bin.zip
unzip -q gradle-8.9-bin.zip

# Generate a clean wrapper jar into the project
cd ~/GitHub-Boss
# minimal temp project method:
mkdir -p /tmp/wrapfix && cd /tmp/wrapfix
echo 'rootProject.name="w"' > settings.gradle.kts
touch build.gradle.kts
echo 'org.gradle.jvmargs=-Xmx512m' > gradle.properties
/tmp/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --no-daemon

# Copy good jar + scripts into your project
cp /tmp/wrapfix/gradle/wrapper/gradle-wrapper.jar ~/GitHub-Boss/gradle/wrapper/
cp /tmp/wrapfix/gradlew ~/GitHub-Boss/
chmod +x ~/GitHub-Boss/gradlew
cd ~/GitHub-Boss
./gradlew --version   # should print Gradle 8.9
```

On **Android Studio / PC** you can also:
```bash
gradle wrapper --gradle-version 8.9
```

## 4. Build debug APK

You need an Android SDK (Android Studio sets this automatically).

```bash
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`

If you see **SDK location not found**:
```bash
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
# Termux example (if you installed sdk):
# echo "sdk.dir=$HOME/android-sdk" > local.properties
```

## 5. Everyday commands

```bash
./gradlew clean assembleDebug --no-daemon
./gradlew :app:compileDebugKotlin --no-daemon
./gradlew installDebug --no-daemon
./gradlew --stop
rm -rf ~/.gradle/caches/ .gradle/
./gradlew clean assembleDebug --no-daemon --refresh-dependencies
```

## 6. Java check

```bash
java -version   # prefer 17
```

## 7. Copy APK to shared storage

```bash
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/GitHub-Boss-debug.apk
```

---

**Tip:** Use `/apk-build-kit/` when AI Studio corrupts Gradle files again.
