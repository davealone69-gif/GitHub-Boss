package com.example.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("github_auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_PAT_TOKEN, token.trim()).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_PAT_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun clearToken() {
        prefs.edit().remove(KEY_PAT_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val KEY_PAT_TOKEN = "github_pat_token"
    }
}
