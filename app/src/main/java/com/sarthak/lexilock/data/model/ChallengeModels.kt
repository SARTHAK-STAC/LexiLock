package com.sarthak.lexilock.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class ChallengeType(val id: String, val title: String, val unitName: String) {
    LEARN_WORDS("learn_words", "Learn words", "words"),
    MASTER_WORDS("master_words", "Master words", "words"),
    PRONUNCIATION("pronunciation", "Pronounce words", "words"),
    REVIEW("review", "Review saved words", "words"),
    STREAK("streak", "Maintain learning streak", "day"),
    DAILY_GOAL("daily_goal", "Complete daily goal", "goal");

    companion object {
        fun fromId(id: String): ChallengeType = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LEARN_WORDS
    }
}

data class DailyChallenge(
    val id: String,
    val date: String, // YYYY-MM-DD
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetCount: Int,
    val xpReward: Int = 50
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("date", date)
        json.put("type", type.id)
        json.put("title", title)
        json.put("description", description)
        json.put("targetCount", targetCount)
        json.put("xpReward", xpReward)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): DailyChallenge? {
            return try {
                val json = JSONObject(jsonStr)
                DailyChallenge(
                    id = json.getString("id"),
                    date = json.getString("date"),
                    type = ChallengeType.fromId(json.getString("type")),
                    title = json.getString("title"),
                    description = json.getString("description"),
                    targetCount = json.getInt("targetCount"),
                    xpReward = json.optInt("xpReward", 50)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ChallengeProgress(
    val challengeId: String,
    val currentCount: Int,
    val isCompleted: Boolean,
    val completedDate: String = ""
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("challengeId", challengeId)
        json.put("currentCount", currentCount)
        json.put("isCompleted", isCompleted)
        json.put("completedDate", completedDate)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ChallengeProgress? {
            return try {
                val json = JSONObject(jsonStr)
                ChallengeProgress(
                    challengeId = json.getString("challengeId"),
                    currentCount = json.getInt("currentCount"),
                    isCompleted = json.getBoolean("isCompleted"),
                    completedDate = json.optString("completedDate", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class LearnerLevel(
    val levelNumber: Int,
    val title: String,
    val minXp: Int,
    val maxXp: Int
) {
    companion object {
        val ALL_LEVELS = listOf(
            LearnerLevel(1, "New Learner", 0, 200),
            LearnerLevel(2, "Word Explorer", 200, 500),
            LearnerLevel(3, "Vocabulary Builder", 500, 1000),
            LearnerLevel(4, "Wordsmith", 1000, 1800),
            LearnerLevel(5, "Lexicon Master", 1800, 3000)
        )

        fun getLevelForXp(xp: Int): LearnerLevel {
            for (level in ALL_LEVELS) {
                if (xp in level.minXp until level.maxXp) {
                    return level
                }
            }
            return ALL_LEVELS.last()
        }
    }
}

data class Badge(
    val id: String,
    val icon: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String = ""
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("icon", icon)
        json.put("name", name)
        json.put("description", description)
        json.put("isUnlocked", isUnlocked)
        json.put("unlockedDate", unlockedDate)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): Badge? {
            return try {
                val json = JSONObject(jsonStr)
                Badge(
                    id = json.getString("id"),
                    icon = json.getString("icon"),
                    name = json.getString("name"),
                    description = json.getString("description"),
                    isUnlocked = json.getBoolean("isUnlocked"),
                    unlockedDate = json.optString("unlockedDate", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
