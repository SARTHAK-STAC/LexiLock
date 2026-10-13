package com.sarthak.lexilock.data.model

enum class MiniGameType(
    val id: String,
    val title: String,
    val subtitle: String,
    val estimatedTime: String,
    val xpBaseReward: Int,
    val iconEmoji: String
) {
    QUICK_MATCH(
        id = "quick_match",
        title = "Quick Match",
        subtitle = "Match words with meanings.",
        estimatedTime = "~45 sec",
        xpBaseReward = 30,
        iconEmoji = "🧩"
    ),
    WORD_RUSH(
        id = "word_rush",
        title = "Word Rush",
        subtitle = "Answer before time runs out.",
        estimatedTime = "~60 sec",
        xpBaseReward = 40,
        iconEmoji = "⚡"
    ),
    FILL_THE_GAP(
        id = "fill_the_gap",
        title = "Fill the Gap",
        subtitle = "Complete the sentence.",
        estimatedTime = "~60 sec",
        xpBaseReward = 35,
        iconEmoji = "✍️"
    ),
    DEFINITION_DUEL(
        id = "definition_duel",
        title = "Definition Duel",
        subtitle = "Choose the correct definition.",
        estimatedTime = "~45 sec",
        xpBaseReward = 30,
        iconEmoji = "🎯"
    ),
    ODD_WORD(
        id = "odd_word",
        title = "Odd Word",
        subtitle = "Find the word that doesn't belong.",
        estimatedTime = "~45 sec",
        xpBaseReward = 35,
        iconEmoji = "🔍"
    ),
    SPELL_IT(
        id = "spell_it",
        title = "Spell It",
        subtitle = "Spell the word from definition.",
        estimatedTime = "~60 sec",
        xpBaseReward = 35,
        iconEmoji = "🔤"
    ),
    MEMORY_FLIP(
        id = "memory_flip",
        title = "Memory Flip",
        subtitle = "Flip cards to find matching pairs.",
        estimatedTime = "~60 sec",
        xpBaseReward = 40,
        iconEmoji = "🃏"
    ),
    WORD_LADDER(
        id = "word_ladder",
        title = "Word Ladder",
        subtitle = "Build a chain of related words.",
        estimatedTime = "~90 sec",
        xpBaseReward = 50,
        iconEmoji = "🪜"
    ),
    GUESS_THE_WORD(
        id = "guess_the_word",
        title = "Guess the Word",
        subtitle = "Deduce the word with progressive clues.",
        estimatedTime = "~60 sec",
        xpBaseReward = 40,
        iconEmoji = "💡"
    );

    companion object {
        fun fromId(id: String): MiniGameType {
            return values().firstOrNull { it.id == id } ?: QUICK_MATCH
        }
    }
}

data class GameResultData(
    val gameType: MiniGameType,
    val correctCount: Int,
    val incorrectCount: Int,
    val xpEarned: Int,
    val accuracyPercent: Int,
    val isNewPersonalBest: Boolean,
    val bestScore: Int,
    val maxCombo: Int
)
