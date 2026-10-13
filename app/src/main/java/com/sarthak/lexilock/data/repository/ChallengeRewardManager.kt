package com.sarthak.lexilock.data.repository

import android.content.Context
import com.sarthak.lexilock.data.model.Badge
import com.sarthak.lexilock.data.model.ChallengeProgress
import com.sarthak.lexilock.data.model.ChallengeType
import com.sarthak.lexilock.data.model.DailyChallenge
import com.sarthak.lexilock.data.model.LearnerLevel
import com.sarthak.lexilock.data.preferences.LexiPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChallengeRewardManager(private val context: Context) {

    private val prefs = LexiPreferences.getInstance(context)

    companion object {
        const val XP_LEARN_WORD = 10
        const val XP_MASTER_WORD = 20
        const val XP_TTS_SESSION = 5
        const val XP_DAILY_CHALLENGE = 50
        const val XP_STREAK_7_DAYS = 100

        @Volatile
        private var INSTANCE: ChallengeRewardManager? = null

        fun getInstance(context: Context): ChallengeRewardManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChallengeRewardManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val allBadges = listOf(
        Badge("badge_first_streak", "🔥", "First Streak", "Maintain a 3-day streak."),
        Badge("badge_week_strong", "⚡", "Week Strong", "Maintain a 7-day streak."),
        Badge("badge_first_25", "📚", "First 25", "Learn 25 words."),
        Badge("badge_100_words", "🎯", "100 Words", "Learn 100 words."),
        Badge("badge_mastermind", "🧠", "Mastermind", "Master 50 words."),
        Badge("badge_pronunciation_pro", "🔊", "Pronunciation Pro", "Complete 25 pronunciation sessions."),
        Badge("badge_challenge_champ", "🏆", "Challenge Champion", "Complete 10 daily challenges."),
        Badge("badge_perfect_week", "🌟", "Perfect Week", "Complete every daily challenge for 7 days.")
    )

    fun getTodayChallenge(): DailyChallenge {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val cachedDate = prefs.challengeDate
        val cachedJson = prefs.challengeJson

        if (cachedDate == todayStr && cachedJson.isNotBlank()) {
            DailyChallenge.fromJson(cachedJson)?.let { return it }
        }

        // Generate rotating daily challenge based on day of year
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val challengePool = listOf(
            DailyChallenge("ch_1", todayStr, ChallengeType.LEARN_WORDS, "Learn 3 words", "Learn 3 new vocabulary words today.", 3, 50),
            DailyChallenge("ch_2", todayStr, ChallengeType.MASTER_WORDS, "Master 2 words", "Mark 2 vocabulary words as mastered.", 2, 50),
            DailyChallenge("ch_3", todayStr, ChallengeType.PRONUNCIATION, "Pronunciation practice", "Pronounce 3 words using audio.", 3, 50),
            DailyChallenge("ch_4", todayStr, ChallengeType.REVIEW, "Review saved words", "Review 3 saved or explore words.", 3, 50),
            DailyChallenge("ch_5", todayStr, ChallengeType.LEARN_WORDS, "Deep dive", "Learn 3 vocabulary words today.", 3, 50),
            DailyChallenge("ch_6", todayStr, ChallengeType.MASTER_WORDS, "Word mastery", "Master 3 vocabulary words.", 3, 50),
            DailyChallenge("ch_7", todayStr, ChallengeType.DAILY_GOAL, "Weekly commitment", "Complete today's vocabulary goal.", 1, 100)
        )

        val selected = challengePool[dayOfYear % challengePool.size].copy(date = todayStr)
        prefs.challengeDate = todayStr
        prefs.challengeJson = selected.toJson()
        prefs.challengeProgressCount = 0
        prefs.challengeCompleted = false
        return selected
    }

    fun getChallengeProgress(): ChallengeProgress {
        val challenge = getTodayChallenge()
        return ChallengeProgress(
            challengeId = challenge.id,
            currentCount = prefs.challengeProgressCount,
            isCompleted = prefs.challengeCompleted,
            completedDate = if (prefs.challengeCompleted) prefs.challengeDate else ""
        )
    }

    /**
     * Records an event and updates challenge progress + XP + badges.
     * Returns true if challenge was just completed with this event.
     */
    fun onEvent(type: ChallengeType, increment: Int = 1): Boolean {
        val challenge = getTodayChallenge()
        var justCompleted = false

        if (!prefs.challengeCompleted && challenge.type == type) {
            val newCount = (prefs.challengeProgressCount + increment).coerceAtMost(challenge.targetCount)
            prefs.challengeProgressCount = newCount
            if (newCount >= challenge.targetCount) {
                prefs.challengeCompleted = true
                justCompleted = true
                prefs.completedChallengesCount = prefs.completedChallengesCount + 1
                prefs.addXp(challenge.xpReward)
            }
        }

        // Check and award badges
        checkBadges()
        return justCompleted
    }

    fun onWordLearned() {
        prefs.addXp(XP_LEARN_WORD)
        onEvent(ChallengeType.LEARN_WORDS, 1)
        checkBadges()
    }

    fun onWordMastered() {
        prefs.addXp(XP_MASTER_WORD)
        onEvent(ChallengeType.MASTER_WORDS, 1)
        checkBadges()
    }

    fun onTtsPronounced() {
        prefs.ttsSessionsCount = prefs.ttsSessionsCount + 1
        prefs.addXp(XP_TTS_SESSION)
        onEvent(ChallengeType.PRONUNCIATION, 1)
        checkBadges()
    }

    fun onReviewPerformed() {
        onEvent(ChallengeType.REVIEW, 1)
    }

    fun getAllBadges(): List<Badge> {
        val unlockedIds = prefs.unlockedBadgesIds
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        return allBadges.map { badge ->
            val unlocked = unlockedIds.contains(badge.id)
            badge.copy(isUnlocked = unlocked, unlockedDate = if (unlocked) todayStr else "")
        }
    }

    fun getCurrentLevel(): LearnerLevel {
        return LearnerLevel.getLevelForXp(prefs.userXp)
    }

    private fun checkBadges() {
        val unlocked = prefs.unlockedBadgesIds.toMutableSet()
        val streak = prefs.currentStreak
        val learned = prefs.totalWordsLearned
        val challenges = prefs.completedChallengesCount
        val ttsCount = prefs.ttsSessionsCount

        if (streak >= 3) unlocked.add("badge_first_streak")
        if (streak >= 7) unlocked.add("badge_week_strong")
        if (learned >= 25) unlocked.add("badge_first_25")
        if (learned >= 100) unlocked.add("badge_100_words")
        if (challenges >= 10) unlocked.add("badge_challenge_champ")
        if (ttsCount >= 25) unlocked.add("badge_pronunciation_pro")

        if (unlocked.size > prefs.unlockedBadgesIds.size) {
            prefs.unlockedBadgesIds = unlocked
        }
    }
}
