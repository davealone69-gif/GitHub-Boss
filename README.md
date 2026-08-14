# GitHub-Boss

**Ultimate GitHub control tool for Android** — no need to open the website for everyday tasks.

## Features

- Login with Personal Access Token (PAT)
- Browse all your repositories (public + private)
- View Issues & Pull Requests
- Monitor GitHub Actions workflow runs
- Create new repositories from the app
- **Star / Unstar** repositories
- **Notifications** feed
- **Search** repositories on GitHub
- **Text → Kotlin Code Maker** (Compose screens, ViewModels, UiState, optional Room/API stubs)
- Termux-ready build commands
- Permanent APK build kit (AI Studio-proof Gradle files)

## Text → Kotlin chances

| Approach | Chance of usable code | Notes |
|----------|----------------------|-------|
| Rule + template maker (what we shipped) | **85–95%** for screens / ViewModels | Offline, fast, predictable |
| Local LLM (Ollama / Dolphin on device) | **70–90%** | Smarter but needs model + RAM |
| Cloud AI (Gemini / Claude / Grok) | **90%+** | Best quality, needs network + key |
| Pure AI Studio auto-gen | **30–60%** | Often breaks Gradle |

The built-in `KotlinCodeMaker` already turns a plain English prompt into real Compose + ViewModel + state files you can copy. Wire a local LLM later for even better results.

## Build it yourself

### On Android Studio / PC
```bash
./gradlew assembleDebug
```

### On Termux (phone)
See **[TERMUX-COMMANDS.md](TERMUX-COMMANDS.md)**.

```bash
pkg install openjdk-17 git
git clone https://github.com/davealone69-gif/GitHub-Boss.git
cd GitHub-Boss
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```

APK → `app/build/outputs/apk/debug/app-debug.apk`

## When Gradle is broken again

Copy files from **`apk-build-kit/`** over the broken ones. Those are protected against AI “improvements”.

## Required PAT scopes

- `repo`
- `workflow`
- `read:user`
- `notifications` (for the notifications tab)

Generate at: https://github.com/settings/tokens
