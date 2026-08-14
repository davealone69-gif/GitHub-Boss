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
- **Text → Kotlin Code Maker** (free template + optional free Gemini)
- Termux-ready build commands
- Permanent APK build kit (AI Studio-proof Gradle files)

## Text → Kotlin (free by design)

| Mode | Cost | Quality |
|------|------|--------|
| **Template maker** (always on) | **$0** | Compose + ViewModel + UiState, offline |
| **Gemini free key** | **$0** | Smarter multi-file Kotlin via Google free tier |
| Grok / Claude API | Paid | Not required |

Setup Gemini in 30 seconds → **[GEMINI-FREE.md](GEMINI-FREE.md)**  
Key: https://aistudio.google.com/apikey

If Gemini fails or you have no key, the app **falls back to the free template**. Competitors that force paid APIs lose here.

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

Copy files from **`apk-build-kit/`** over the broken ones.

## Required PAT scopes

- `repo`
- `workflow`
- `read:user`
- `notifications`

Generate at: https://github.com/settings/tokens
