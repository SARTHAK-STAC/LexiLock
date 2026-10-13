package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.domain.model.WordItem

data class BattleChallenge(
    val challengerName: String,
    val challengerScore: Int,
    val wordIds: List<Long>,
    val codeString: String
)

data class BattleShowdown(
    val challengerName: String,
    val challengerScore: Int,
    val userScore: Int,
    val isUserWinner: Boolean,
    val isDraw: Boolean
)

object FriendBattleManager {

    fun createChallengeCode(
        userName: String,
        score: Int,
        words: List<WordItem>
    ): String {
        val idsStr = words.map { it.id }.joinToString(",")
        val cleanName = userName.ifBlank { "Challenger" }.replace(":", "_")
        return "LEXI-BATTLE:$cleanName:$score:$idsStr"
    }

    fun parseChallengeCode(code: String): BattleChallenge? {
        val trimmed = code.trim()
        if (!trimmed.startsWith("LEXI-BATTLE:")) return null

        val parts = trimmed.removePrefix("LEXI-BATTLE:").split(":")
        if (parts.size < 3) return null

        val name = parts[0].ifBlank { return null }
        val score = parts[1].toIntOrNull() ?: return null
        if (score < 0) return null
        val ids = parts[2].split(",").mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return null

        return BattleChallenge(
            challengerName = name,
            challengerScore = score,
            wordIds = ids,
            codeString = trimmed
        )
    }

    fun compareResult(
        challenge: BattleChallenge,
        userScore: Int
    ): BattleShowdown {
        val isUserWinner = userScore > challenge.challengerScore
        val isDraw = userScore == challenge.challengerScore
        return BattleShowdown(
            challengerName = challenge.challengerName,
            challengerScore = challenge.challengerScore,
            userScore = userScore,
            isUserWinner = isUserWinner,
            isDraw = isDraw
        )
    }
}
