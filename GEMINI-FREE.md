# Free Gemini for GitHub-Boss Code Maker

You do **not** need to pay. Google gives a free API key with rate limits that are plenty for generating Kotlin screens.

## 1. Get a free key (30 seconds)

1. Open **https://aistudio.google.com/apikey**
2. Sign in with any Google account
3. Click **Create API key**
4. Copy the key (starts with `AIza...`)

## 2. Put it in the app

In the **Builder** tab:

- Paste the key in **Gemini API key**
- Turn on **Use Gemini (free)**
- Hit **Generate Kotlin**

If Gemini is rate-limited or the key is wrong, the app **automatically falls back** to the built-in free template maker. You never get a blank screen.

## 3. How the maker works

| Mode | Cost | Quality |
|------|------|--------|
| **Template** (default) | $0 forever | Solid Compose + ViewModel + UiState |
| **Gemini free key** | $0 (Google free tier) | Smarter, more complete files |
| Grok API | Paid | Not required |

## Tips

- Free tier has daily / per-minute limits. If you hit them, wait a bit or use Template.
- Never commit your key to GitHub. The app stores it only on your phone.
- Model used: `gemini-2.0-flash` (fast, free-tier friendly).

That’s it. Free beats paid competitors for this use case.
