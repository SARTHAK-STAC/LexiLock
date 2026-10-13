package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.domain.model.WordItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MysteryWordState(
    val wordItem: WordItem,
    val cluesRevealed: Int, // 1 to 4
    val isSolved: Boolean,
    val isGuessedCorrectly: Boolean,
    val earnedXp: Int,
    val maskedExample: String,
    val letterHint: String
)

class MysteryWordManager(
    private val prefs: LexiPreferences,
    private val repository: WordRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayState(): MysteryWordState {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val (cachedId, cachedClues, isSolved) = prefs.getMysteryWordState(todayStr)

        val wordItem = if (cachedId > 0) {
            repository.getWordById(cachedId) ?: getDeterministicDailyWord()
        } else {
            getDeterministicDailyWord()
        }

        val safeWord = wordItem.word.trim()
        val maskedExample = wordItem.exampleSentence.replace(
            Regex(Regex.escape(safeWord), RegexOption.IGNORE_CASE),
            "[ ______ ]"
        )

        val letterHint = "${safeWord.first().uppercaseChar()}" + " _".repeat((safeWord.length - 1).coerceAtLeast(0)) + " (${safeWord.length} letters)"

        return MysteryWordState(
            wordItem = wordItem,
            cluesRevealed = cachedClues.coerceIn(1, 4),
            isSolved = isSolved,
            isGuessedCorrectly = isSolved,
            earnedXp = if (isSolved) calculateReward(cachedClues) else 0,
            maskedExample = maskedExample,
            letterHint = letterHint
        )
    }

    fun revealNextClue(): MysteryWordState {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val current = getTodayState()
        if (current.isSolved || current.cluesRevealed >= 4) return current

        val nextClues = current.cluesRevealed + 1
        prefs.saveMysteryWordState(
            dateStr = todayStr,
            wordId = current.wordItem.id,
            cluesRevealed = nextClues,
            isSolved = false
        )
        return getTodayState()
    }

    fun attemptGuess(guess: String): Pair<Boolean, MysteryWordState> {
        val current = getTodayState()
        if (current.isSolved) return Pair(true, current)

        val cleanGuess = guess.trim().lowercase()
        val target = current.wordItem.word.trim().lowercase()

        val isCorrect = cleanGuess == target
        if (isCorrect) {
            val todayStr = dateFormat.format(Calendar.getInstance().time)
            val xp = calculateReward(current.cluesRevealed)
            prefs.saveMysteryWordState(
                dateStr = todayStr,
                wordId = current.wordItem.id,
                cluesRevealed = current.cluesRevealed,
                isSolved = true
            )
            prefs.addXp(xp)
            repository.recordGameWordCorrect(current.wordItem.id)
        }
        return Pair(isCorrect, getTodayState())
    }

    fun revealWord(): MysteryWordState {
        val current = getTodayState()
        if (current.isSolved) return current

        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val xp = 5 // Base surrender reveal reward
        prefs.saveMysteryWordState(
            dateStr = todayStr,
            wordId = current.wordItem.id,
            cluesRevealed = 4,
            isSolved = true
        )
        prefs.addXp(xp)
        return getTodayState()
    }

    private fun calculateReward(cluesUsed: Int): Int {
        return when (cluesUsed) {
            1 -> 50
            2 -> 40
            3 -> 30
            4 -> 20
            else -> 10
        }
    }

    private fun getDeterministicDailyWord(): WordItem {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val words = repository.getAllWords()
        val index = (year * 365 + dayOfYear * 7) % (words.size.coerceAtLeast(1))
        return words[index.coerceIn(0, words.lastIndex)]
    }
}
