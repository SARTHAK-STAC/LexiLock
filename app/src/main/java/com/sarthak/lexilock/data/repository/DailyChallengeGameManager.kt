package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyChallengeInfo(
    val gameType: MiniGameType,
    val dateString: String,
    val isCompleted: Boolean,
    val savedScore: Int,
    val bonusXp: Int = 50,
    val estimatedTime: String = "5 minutes"
)

class DailyChallengeGameManager(
    private val prefs: LexiPreferences
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayChallengeInfo(): DailyChallengeInfo {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        // Rotate through all arcade games deterministically
        val games = listOf(
            MiniGameType.QUICK_MATCH,
            MiniGameType.WORD_RUSH,
            MiniGameType.FILL_THE_GAP,
            MiniGameType.DEFINITION_DUEL,
            MiniGameType.ODD_WORD,
            MiniGameType.SPELL_IT,
            MiniGameType.MEMORY_FLIP,
            MiniGameType.WORD_LADDER
        )
        val game = games[(year * 365 + dayOfYear) % games.size]

        val isCompleted = prefs.isDailyChallengeCompletedToday(todayStr)
        val score = prefs.getDailyChallengeScore()

        return DailyChallengeInfo(
            gameType = game,
            dateString = todayStr,
            isCompleted = isCompleted,
            savedScore = score,
            bonusXp = 50
        )
    }

    fun completeDailyChallenge(score: Int): Int {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val alreadyDone = prefs.isDailyChallengeCompletedToday(todayStr)

        return if (!alreadyDone) {
            prefs.markDailyChallengeCompleted(todayStr, score)
            prefs.addXp(50)
            // Also contribute to weekly league
            val cal = Calendar.getInstance()
            val weekKey = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
            prefs.addLeagueXp(weekKey, 50)
            50
        } else {
            0 // Anti-farming: 0 XP on replay
        }
    }
}
