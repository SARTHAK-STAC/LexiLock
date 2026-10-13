package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences

data class PlayerTitleItem(
    val id: String,
    val title: String,
    val description: String,
    val unlockCondition: String,
    val isUnlocked: Boolean
)

object PlayerTitleManager {

    val ALL_TITLES = listOf(
        PlayerTitleItem(
            id = "title_beginner",
            title = "WORD BEGINNER",
            description = "Embarked on the daily vocabulary path.",
            unlockCondition = "Available by default",
            isUnlocked = true
        ),
        PlayerTitleItem(
            id = "title_quiz_runner",
            title = "QUIZ RUNNER",
            description = "Completed at least 5 Lexi Arcade sessions.",
            unlockCondition = "Play 5 arcade games",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_word_hunter",
            title = "WORD HUNTER",
            description = "Discovered and saved 10 vocabulary words.",
            unlockCondition = "Save 10 words to your personal shelf",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_lexi_explorer",
            title = "LEXI EXPLORER",
            description = "Explored words across 4 different categories.",
            unlockCondition = "Master words in 4 categories",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_precision_master",
            title = "PRECISION MASTER",
            description = "Achieved 100% accuracy in 3 consecutive games.",
            unlockCondition = "Complete 3 games with 100% accuracy",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_speed_demon",
            title = "SPEED DEMON",
            description = "Scored 10+ points in Word Rush with prompt response.",
            unlockCondition = "Achieve score ≥ 10 in Word Rush",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_wordsmith",
            title = "WORDSMITH",
            description = "Mastered at least 25 vocabulary words.",
            unlockCondition = "Master 25 words",
            isUnlocked = false
        ),
        PlayerTitleItem(
            id = "title_lexi_master",
            title = "LEXI MASTER",
            description = "Reached Level 5 with an unbroken 7-day streak.",
            unlockCondition = "Reach Level 5 and 7-day streak",
            isUnlocked = false
        )
    )

    fun getTitles(prefs: LexiPreferences, repository: WordRepository): List<PlayerTitleItem> {
        val gamesPlayed = prefs.arcadeGamesPlayed
        val savedWordsCount = repository.getSavedWords().size
        val masteredWordsCount = repository.getMasteredWordsCount()
        val perfectGames = prefs.perfectGamesCount
        val streak = prefs.currentStreak
        val level = LevelManager.getLevelInfo(prefs.userXp).levelNumber
        val wordRushBest = prefs.getArcadeBestScore("word_rush")

        return ALL_TITLES.map { item ->
            val unlocked = when (item.id) {
                "title_beginner" -> true
                "title_quiz_runner" -> gamesPlayed >= 5
                "title_word_hunter" -> savedWordsCount >= 10
                "title_lexi_explorer" -> masteredWordsCount >= 5
                "title_precision_master" -> perfectGames >= 3
                "title_speed_demon" -> wordRushBest >= 10
                "title_wordsmith" -> masteredWordsCount >= 25
                "title_lexi_master" -> level >= 5 && streak >= 7
                else -> false
            }
            item.copy(isUnlocked = unlocked)
        }
    }

    fun getActiveTitle(prefs: LexiPreferences): String {
        val selected = prefs.selectedPlayerTitle
        return if (selected.isNotBlank()) selected else "WORD BEGINNER"
    }

    fun setActiveTitle(prefs: LexiPreferences, title: String) {
        prefs.selectedPlayerTitle = title
    }
}
