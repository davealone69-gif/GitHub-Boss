# GitHub-Boss

**Ultimate GitHub control tool for Android** — no need to open the website for everyday tasks.

## Features

- Login with Personal Access Token (PAT)
- Browse all your repositories (public + private)
- View Issues & Pull Requests
- Monitor GitHub Actions workflow runs
- Create new repositories from the app
- Simple App Builder / file generator (bonus)

## Build it yourself

### On Android Studio / PC
```bash
./gradlew assembleDebug
```

### On Termux (phone)
See **[TERMUX-COMMANDS.md](TERMUX-COMMANDS.md)** for the full list of working commands.

Quick version:
```bash
pkg install openjdk-17 git
git clone https://github.com/davealone69-gif/GitHub-Boss.git
cd GitHub-Boss
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```

APK → `app/build/outputs/apk/debug/app-debug.apk`

## When Gradle is broken again (AI Studio did it)

Copy the permanent working files from **`apk-build-kit/`** over the broken ones.  
Those files are intentionally protected against AI “improvements”.

## Required PAT scopes

- `repo`
- `workflow`
- `read:user`

Generate at: https://github.com/settings/tokens
