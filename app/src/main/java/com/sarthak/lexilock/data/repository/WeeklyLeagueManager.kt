package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences
import java.util.Calendar

data class LeagueParticipant(
    val rank: Int,
    val name: String,
    val xp: Int,
    val isUser: Boolean,
    val isPromotionZone: Boolean
)

data class WeeklyLeagueState(
    val weekLabel: String,
    val daysRemaining: Int,
    val userRank: Int,
    val userXp: Int,
    val participants: List<LeagueParticipant>,
    val isPromoted: Boolean
)

object WeeklyLeagueManager {

    private val COHORT_NAMES = listOf(
        "Elena V.", "Marcus K.", "Liam Chen", "Chloe Bennett",
        "Julian Rossi", "Sophia Patel", "David O'Connor", "Aria Thorne", "Zane Miller"
    )

    fun getLeagueState(prefs: LexiPreferences): WeeklyLeagueState {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
        val weekKey = "$year-W$week"

        // Calculate days remaining in league (ends Sunday night)
        val daysRemaining = if (dayOfWeek == Calendar.SUNDAY) 1 else (Calendar.SATURDAY - dayOfWeek + 2)

        val (_, userLeagueXp) = prefs.getLeagueData(weekKey)

        // Baseline progression for simulated cohort based on current day of week
        // Monday (day 2) = smaller XP, Saturday/Sunday = higher XP
        val dayMultiplier = when (dayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 3
        }

        // Deterministic benchmarks for cohort
        val cohortList = COHORT_NAMES.mapIndexed { idx, name ->
            val baseScale = 45 - (idx * 3) // 45, 42, 39...
            val xp = baseScale * dayMultiplier + ((idx * 17 + week) % 25)
            name to xp
        }

        // Add user and sort
        val combined = mutableListOf<Triple<String, Int, Boolean>>()
        combined.add(Triple("You", userLeagueXp, true))
        cohortList.forEach { (name, xp) ->
            combined.add(Triple(name, xp, false))
        }

        val sorted = combined.sortedByDescending { it.second }
        var userRank = 1

        val finalParticipants = sorted.mapIndexed { index, item ->
            val rank = index + 1
            if (item.third) userRank = rank
            LeagueParticipant(
                rank = rank,
                name = item.first,
                xp = item.second,
                isUser = item.third,
                isPromotionZone = rank <= 3
            )
        }

        return WeeklyLeagueState(
            weekLabel = "Week $week Cohort",
            daysRemaining = daysRemaining,
            userRank = userRank,
            userXp = userLeagueXp,
            participants = finalParticipants,
            isPromoted = userRank <= 3
        )
    }
}
