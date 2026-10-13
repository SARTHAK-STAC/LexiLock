package com.sarthak.lexilock.data.repository

import android.content.Context
import com.sarthak.lexilock.data.model.GameResultData
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.util.LexiTimeProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MiniGameManager(private val context: Context) {

    private val prefs = LexiPreferences.getInstance(context)
    private val wordRepository = WordRepository(context)
    private val challengeManager = ChallengeRewardManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: MiniGameManager? = null

        fun getInstance(context: Context): MiniGameManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MiniGameManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    fun getTodayFeaturedGame(): MiniGameType {
        val cal = LexiTimeProvider.getCalendar()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val allGames = MiniGameType.values()
        return allGames[dayOfYear % allGames.size]
    }

    /**
     * Calculates XP with progressive combo bonus and caps it against infinite farming.
     * Combo mechanic:
     * 1 correct: +10 XP
     * 2 combo: +12 XP
     * 3 combo: +15 XP
     * 4 combo: +18 XP
     * 5+ combo: +20 XP
     */
    fun calculateComboXp(combo: Int): Int {
        return when (combo) {
            0, 1 -> 10
            2 -> 12
            3 -> 15
            4 -> 18
            else -> 20
        }
    }

    /**
     * Records final game result, awards bounded XP, updates streaks and personal bests.
     */
    fun recordGameFinished(
        gameType: MiniGameType,
        correctCount: Int,
        incorrectCount: Int,
        totalXpAccumulated: Int,
        maxCombo: Int
    ): GameResultData {
        val totalAnswers = correctCount + incorrectCount
        val accuracy = if (totalAnswers > 0) ((correctCount.toFloat() / totalAnswers.toFloat()) * 100).toInt() else 0

        // Bonus XP:
        // Completed: +20 XP
        // Perfect game: +30 XP (if at least 3 questions answered and 0 errors)
        var bonusXp = 20
        if (correctCount >= 3 && incorrectCount == 0) {
            bonusXp += 30
        }

        // Daily First Game bonus: +10 XP
        val todayStr = LexiTimeProvider.getTodayDateString()
        if (prefs.arcadeFirstPlayBonusDate != todayStr) {
            bonusXp += 10
            prefs.arcadeFirstPlayBonusDate = todayStr
        }

        // Capped total XP for this session to prevent farming (max 150 per game session)
        val sessionXp = (totalXpAccumulated + bonusXp).coerceIn(0, 150)
        prefs.addXp(sessionXp)

        // Increment total games played
        prefs.arcadeGamesPlayed = prefs.arcadeGamesPlayed + 1

        // Update arcade game streak
        updateGameStreak(todayStr)

        // Update personal best
        val previousBest = prefs.getArcadeBestScore(gameType.id)
        val isNewBest = correctCount > previousBest
        if (isNewBest) {
            prefs.setArcadeBestScore(gameType.id, correctCount)
        }
        val currentBest = maxOf(previousBest, correctCount)

        // Integrate with daily challenge if challenge is LEARN_WORDS or REVIEW
        val dailyChallenge = challengeManager.getTodayChallenge()
        val currentProgress = challengeManager.getChallengeProgress()
        if (!currentProgress.isCompleted && correctCount > 0) {
            prefs.challengeProgressCount = (currentProgress.currentCount + 1).coerceAtMost(dailyChallenge.targetCount)
            if (prefs.challengeProgressCount >= dailyChallenge.targetCount) {
                prefs.challengeCompleted = true
                prefs.addXp(dailyChallenge.xpReward)
                prefs.completedChallengesCount = prefs.completedChallengesCount + 1
            }
        }

        return GameResultData(
            gameType = gameType,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            xpEarned = sessionXp,
            accuracyPercent = accuracy,
            isNewPersonalBest = isNewBest,
            bestScore = currentBest,
            maxCombo = maxCombo
        )
    }

    private fun updateGameStreak(todayStr: String) {
        val lastDate = prefs.arcadeLastPlayedDate
        if (lastDate == todayStr) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance().apply {
            time = sdf.parse(todayStr) ?: Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var newStreak = 1
        if (lastDate.isNotBlank()) {
            val lastCal = Calendar.getInstance().apply {
                time = sdf.parse(lastDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffDays = ((todayCal.timeInMillis - lastCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            newStreak = when (diffDays) {
                1 -> prefs.arcadeGameStreak + 1
                0 -> prefs.arcadeGameStreak
                else -> 1
            }
        }

        prefs.arcadeGameStreak = newStreak
        prefs.arcadeLastPlayedDate = todayStr

        // Also ensure main streak is updated if user played a game today
        wordRepository.updateStreakOnLearning(todayStr)
    }

    fun onWordAnsweredCorrectly(wordId: Long) {
        wordRepository.recordGameWordCorrect(wordId)
    }
}
