package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences

data class ArcadeProgressionInfo(
    val arcadeLevel: Int,
    val currentLevelXp: Int,
    val xpForNextLevel: Int,
    val progressFraction: Float,
    val totalArcadeXp: Int,
    val title: String,
    val nextRewardHint: String
)

object ArcadeProgressionManager {

    private val LEVEL_THRESHOLDS = listOf(
        0,     // Level 1: Novice
        250,   // Level 2: Challenger
        600,   // Level 3: Tactician
        1100,  // Level 4: Master
        1800,  // Level 5: Grandmaster
        2700,  // Level 6: Virtuoso
        3800,  // Level 7: Champion
        5100,  // Level 8: Legend
        6600,  // Level 9: Sovereign
        8500   // Level 10: Lexi Immortal
    )

    private val LEVEL_TITLES = listOf(
        "Novice",
        "Challenger",
        "Tactician",
        "Master",
        "Grandmaster",
        "Virtuoso",
        "Champion",
        "Legend",
        "Sovereign",
        "Lexi Immortal"
    )

    private val REWARD_HINTS = listOf(
        "Profile Badge: Challenger",
        "Arcade Title: QUIZ RUNNER",
        "Card Style: Obsidian Slate",
        "Arcade Title: PRECISION MASTER",
        "Wallpaper Drop: Carbon Noir",
        "Arcade Title: SPEED DEMON",
        "Profile Badge: Grand Champion",
        "Arcade Title: LEXI MASTER",
        "Cosmetic: Golden Serif Monogram",
        "Max Arcade Tier Reached"
    )

    fun getProgression(prefs: LexiPreferences): ArcadeProgressionInfo {
        val totalArcadeXp = prefs.arcadeTotalXp
        var level = 1
        for (i in LEVEL_THRESHOLDS.indices.reversed()) {
            if (totalArcadeXp >= LEVEL_THRESHOLDS[i]) {
                level = i + 1
                break
            }
        }
        level = level.coerceIn(1, 10)

        val curThreshold = LEVEL_THRESHOLDS[level - 1]
        val nextThreshold = if (level < 10) LEVEL_THRESHOLDS[level] else curThreshold + 2000
        val xpInLevel = (totalArcadeXp - curThreshold).coerceAtLeast(0)
        val needed = (nextThreshold - curThreshold).coerceAtLeast(1)
        val fraction = (xpInLevel.toFloat() / needed.toFloat()).coerceIn(0f, 1f)

        return ArcadeProgressionInfo(
            arcadeLevel = level,
            currentLevelXp = xpInLevel,
            xpForNextLevel = needed,
            progressFraction = fraction,
            totalArcadeXp = totalArcadeXp,
            title = LEVEL_TITLES.getOrElse(level - 1) { "Master" },
            nextRewardHint = REWARD_HINTS.getOrElse(level - 1) { "Arcade Trophy" }
        )
    }
}
