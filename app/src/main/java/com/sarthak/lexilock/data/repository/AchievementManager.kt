package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val requiredProgress: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val rewardXp: Int = 30
)

object AchievementManager {

    fun checkAndUnlockAll(
        prefs: LexiPreferences,
        repository: WordRepository
    ): List<AchievementItem> {
        val learnedCount = repository.getLearnedWordsCount()
        val masteredCount = repository.getMasteredWordsCount()
        val streak = maxOf(prefs.currentStreak, prefs.longestStreak)
        val arcadeGames = prefs.arcadeGamesPlayed
        val perfectGames = prefs.perfectGamesCount
        val mysteryCount = prefs.mysteryWordsSolvedCount

        val list = mutableListOf<AchievementItem>()

        fun evaluate(
            id: String,
            title: String,
            description: String,
            emoji: String,
            req: Int,
            curr: Int,
            xp: Int = 30
        ) {
            val unlocked = prefs.getUnlockedAchievements().contains(id) || curr >= req
            if (curr >= req && !prefs.getUnlockedAchievements().contains(id)) {
                prefs.unlockAchievement(id)
                prefs.addXp(xp)
            }
            list.add(
                AchievementItem(
                    id = id,
                    title = title,
                    description = description,
                    iconEmoji = emoji,
                    requiredProgress = req,
                    currentProgress = curr.coerceAtMost(req),
                    isUnlocked = unlocked,
                    rewardXp = xp
                )
            )
        }

        evaluate("FIRST_WORD", "First Word", "Learn your first vocabulary word", "🌱", 1, learnedCount, 20)
        evaluate("WORD_COLLECTOR", "Word Collector", "Add 50 words to your repertoire", "📚", 50, learnedCount, 50)
        evaluate("CENTURY", "Century", "Learn 100 vocabulary words", "🏛️", 100, learnedCount, 100)
        evaluate("QUIZ_MASTER", "Quiz Master", "Complete 10 games with 100% accuracy", "🎯", 10, perfectGames, 75)
        evaluate("STREAK_STARTER", "Streak Starter", "Maintain a 3-day learning streak", "🔥", 3, streak, 30)
        evaluate("ONE_WEEK", "One Week", "Maintain a 7-day learning streak", "⚡", 7, streak, 50)
        evaluate("MONTH", "Month Milestone", "Reach a 30-day streak", "🌟", 30, streak, 120)
        evaluate("CENTURION", "Centurion", "Achieve a monumental 100-day streak", "👑", 100, streak, 250)
        evaluate("ARCADE_BEGINNER", "Arcade Beginner", "Play your first Lexi Arcade game", "🎮", 1, arcadeGames, 20)
        evaluate("ARCADE_MASTER", "Arcade Master", "Achieve a personal best score of 10+", "🏆", 10, prefs.getOverallBestScore(), 60)
        evaluate("MYSTERY_SOLVER", "Mystery Solver", "Solve 10 daily mystery words", "🔍", 10, mysteryCount, 80)
        evaluate("WORDSMITH", "Wordsmith", "Master 100 vocabulary words", "✍️", 100, masteredCount, 150)

        return list
    }
}
