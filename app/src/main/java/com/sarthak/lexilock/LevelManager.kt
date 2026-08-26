package com.sarthak.lexilock

object LevelManager {

    fun calculateLevel(score: Int): String {

        return when {
            score <= 1 -> "beginner"
            score <= 3 -> "intermediate"
            else -> "advanced"
        }
    }
}