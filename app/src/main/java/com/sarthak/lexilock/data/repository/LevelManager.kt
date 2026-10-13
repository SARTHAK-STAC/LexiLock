package com.sarthak.lexilock.data.repository

data class LevelInfo(
    val levelNumber: Int,
    val title: String,
    val currentLevelXp: Int,
    val xpForNextLevel: Int,
    val progressFraction: Float,
    val totalXp: Int
)

object LevelManager {

    private val THRESHOLDS = listOf(
        0 to "New Learner",
        100 to "Word Apprentice",
        250 to "Vocab Enthusiast",
        450 to "Language Crafter",
        700 to "Rhetoric Scholar",
        1000 to "Editorial Stylist",
        1400 to "Lexicon Master",
        1900 to "Wordsmith Laureate",
        2500 to "Grand Polymath",
        3200 to "Living Lexicon"
    )

    fun getLevelInfo(totalXp: Int): LevelInfo {
        val safeXp = totalXp.coerceAtLeast(0)

        for (i in THRESHOLDS.indices.reversed()) {
            val (minXp, title) = THRESHOLDS[i]
            if (safeXp >= minXp) {
                val nextThreshold = if (i < THRESHOLDS.size - 1) THRESHOLDS[i + 1].first else minXp + 1000
                val span = (nextThreshold - minXp).coerceAtLeast(1)
                val inLevel = safeXp - minXp
                val frac = (inLevel.toFloat() / span.toFloat()).coerceIn(0f, 1f)
                return LevelInfo(
                    levelNumber = i + 1,
                    title = title,
                    currentLevelXp = inLevel,
                    xpForNextLevel = span,
                    progressFraction = frac,
                    totalXp = safeXp
                )
            }
        }

        return LevelInfo(1, "New Learner", 0, 100, 0f, 0)
    }
}
