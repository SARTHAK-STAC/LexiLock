package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences

data class LearnerProfile(
    val title: String,
    val headline: String,
    val description: String,
    val strength: String,
    val weakness: String,
    val recommendedAction: String,
    val accuracyPercent: Int,
    val wordsLearned: Int,
    val gamesCompleted: Int,
    val streakDays: Int
)

object LearnerProfileManager {

    fun determineProfile(
        prefs: LexiPreferences,
        repository: WordRepository
    ): LearnerProfile {
        val wordsLearned = repository.getLearnedWordsCount()
        val mastered = repository.getMasteredWordsCount()
        val games = prefs.arcadeGamesPlayed
        val streak = prefs.currentStreak
        val bestScore = prefs.getOverallBestScore()

        val accuracy = if (games > 0) {
            val ratio = (mastered.toFloat() / maxOf(1, wordsLearned)).coerceIn(0.5f, 1.0f)
            (ratio * 100).toInt()
        } else {
            100
        }

        return when {
            mastered >= 15 || (wordsLearned >= 20 && mastered * 2 >= wordsLearned) -> {
                LearnerProfile(
                    title = "THE WORDSMITH",
                    headline = "Precision and depth over speed.",
                    description = "You take pride in true retention. You rarely skim—you internalize words until they become part of your everyday lexicon.",
                    strength = "Deep vocabulary retention & authentic application.",
                    weakness = "Can hesitate when rapid recall is tested.",
                    recommendedAction = "Challenge your reaction time in Word Rush or Definition Duel.",
                    accuracyPercent = accuracy,
                    wordsLearned = wordsLearned,
                    gamesCompleted = games,
                    streakDays = streak
                )
            }
            streak >= 5 -> {
                LearnerProfile(
                    title = "THE CONSISTENT LEARNER",
                    headline = "Habit is your superpower.",
                    description = "You believe that compounding effort beats erratic bursts. Your ritual of daily exposure ensures steady, lasting progress.",
                    strength = "Unwavering study rhythm and discipline.",
                    weakness = "May stick to comfort zones without pushing into expert words.",
                    recommendedAction = "Tackle an Expert difficulty word in Explore today.",
                    accuracyPercent = accuracy,
                    wordsLearned = wordsLearned,
                    gamesCompleted = games,
                    streakDays = streak
                )
            }
            games >= 10 || bestScore >= 40 -> {
                LearnerProfile(
                    title = "THE SPEED LEARNER",
                    headline = "Sharp instincts under time pressure.",
                    description = "Your mind accelerates in rapid environments. You decipher semantics instinctively and thrive during quick, intense challenges.",
                    strength = "Instant cognitive association and agility.",
                    weakness = "Nuances in subtle definitions might be skipped.",
                    recommendedAction = "Solve today's Mystery Word to test deductive precision.",
                    accuracyPercent = accuracy,
                    wordsLearned = wordsLearned,
                    gamesCompleted = games,
                    streakDays = streak
                )
            }
            wordsLearned >= 25 -> {
                LearnerProfile(
                    title = "THE EXPLORER",
                    headline = "Insatiable curiosity across disciplines.",
                    description = "You love traversing varied lexical domains—from literary rhetoric to technical precision. Breadth drives your passion.",
                    strength = "Broad lexical range and cross-domain curiosity.",
                    weakness = "Lower rate of deep word mastery.",
                    recommendedAction = "Mark 3 explored words as 'Mastered' to deepen your recall.",
                    accuracyPercent = accuracy,
                    wordsLearned = wordsLearned,
                    gamesCompleted = games,
                    streakDays = streak
                )
            }
            else -> {
                LearnerProfile(
                    title = "THE PRECISION LEARNER",
                    headline = "Methodical, attentive, and intentional.",
                    description = "You approach every new word with care, absorbing pronunciations, etymologies, and contextual syntax before moving ahead.",
                    strength = "Zero guesswork and meticulous attention to detail.",
                    weakness = "Volume of vocabulary intake can be modest.",
                    recommendedAction = "Play Quick Match to connect multiple words at once.",
                    accuracyPercent = accuracy,
                    wordsLearned = wordsLearned,
                    gamesCompleted = games,
                    streakDays = streak
                )
            }
        }
    }
}
