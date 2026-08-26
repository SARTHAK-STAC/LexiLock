package com.sarthak.lexilock

import android.content.Context

object UserPreferences {

    private const val PREFS_NAME = "lexilock_preferences"

    private const val LEVEL_KEY = "user_level"
    private const val ONBOARDING_KEY = "onboarding_completed"

    // Save user's English level
    fun saveLevel(context: Context, level: String) {

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        preferences.edit()
            .putString(LEVEL_KEY, level)
            .apply()
    }

    // Get user's English level
    fun getLevel(context: Context): String {

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return preferences.getString(
            LEVEL_KEY,
            "beginner"
        ) ?: "beginner"
    }

    // Mark onboarding as completed
    fun completeOnboarding(context: Context) {

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean(ONBOARDING_KEY, true)
            .apply()
    }

    // Check whether onboarding has been completed
    fun isOnboardingCompleted(context: Context): Boolean {

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return preferences.getBoolean(
            ONBOARDING_KEY,
            false
        )
    }
}