package com.example.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("github_auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_PAT_TOKEN, token.trim()).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_PAT_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun clearToken() {
        prefs.edit().remove(KEY_PAT_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    // --- Free Gemini API key (Google AI Studio) ---

    fun saveGeminiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun getGeminiKey(): String? {
        return prefs.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }
    }

    fun clearGeminiKey() {
        prefs.edit().remove(KEY_GEMINI).apply()
    }

    fun hasGeminiKey(): Boolean = !getGeminiKey().isNullOrBlank()

    companion object {
        private const val KEY_PAT_TOKEN = "github_pat_token"
        private const val KEY_GEMINI = "gemini_api_key"
    }
}
