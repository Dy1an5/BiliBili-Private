package com.example.bilidynamic1.data.manager

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREFS_NAME = "user"
    private const val KEY_COOKIE = "cookie"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveCookie(cookie: String) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply()
    }

    fun getCookie(): String {
        return prefs.getString(KEY_COOKIE, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return getCookie().isNotEmpty()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}