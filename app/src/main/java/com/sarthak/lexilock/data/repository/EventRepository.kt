package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.domain.model.WordItem
import java.util.Calendar

data class LimitedEvent(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val categoryFilter: String?,
    val bonusXp: Int,
    val targetWordCount: Int,
    val isCompleted: Boolean,
    val currentProgress: Int,
    val daysRemaining: Int
)

class EventRepository(
    private val prefs: LexiPreferences,
    private val wordRepository: WordRepository
) {

    private val EVENTS_CYCLE = listOf(
        Triple("EVENT_INTERVIEWS", "Interview Mastery Week", "High-frequency corporate & interview rhetoric"),
        Triple("EVENT_BUSINESS", "Executive Lexicon", "Precision vocabulary for leadership & strategy"),
        Triple("EVENT_ACADEMIC", "Scholarly Discourse", "Nuanced words for formal writing and debate"),
        Triple("EVENT_COMMUNICATION", "Art of Eloquence", "Words that elevate persuasion and expression"),
        Triple("EVENT_EMOTIONS", "Nuance & Psychology", "Introspective terms describing human condition")
    )

    fun getCurrentEvent(): LimitedEvent {
        val cal = Calendar.getInstance()
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysRemaining = if (dayOfWeek == Calendar.SUNDAY) 1 else (Calendar.SATURDAY - dayOfWeek + 2)

        val cycleIndex = (week) % EVENTS_CYCLE.size
        val (id, title, desc) = EVENTS_CYCLE[cycleIndex]

        val category = when (id) {
            "EVENT_INTERVIEWS" -> "Interviews"
            "EVENT_BUSINESS" -> "Business"
            "EVENT_ACADEMIC" -> "Academic"
            "EVENT_COMMUNICATION" -> "Communication"
            "EVENT_EMOTIONS" -> "Emotions"
            else -> null
        }

        val completed = prefs.getCompletedEvents().contains(id)
        val categoryWords = if (category != null) {
            wordRepository.getWordsByCategory(category)
        } else {
            wordRepository.getAllWords().take(10)
        }

        val masteredInCat = categoryWords.count { it.isMastered }
        val target = minOf(5, categoryWords.size.coerceAtLeast(1))

        if (masteredInCat >= target && !completed) {
            prefs.markEventCompleted(id)
            prefs.addXp(100)
        }

        return LimitedEvent(
            id = id,
            title = title,
            subtitle = "Limited-Time Challenge",
            description = desc,
            categoryFilter = category,
            bonusXp = 100,
            targetWordCount = target,
            isCompleted = completed || masteredInCat >= target,
            currentProgress = masteredInCat.coerceAtMost(target),
            daysRemaining = daysRemaining
        )
    }

    fun getEventWords(): List<WordItem> {
        val event = getCurrentEvent()
        return if (event.categoryFilter != null) {
            wordRepository.getWordsByCategory(event.categoryFilter)
        } else {
            wordRepository.getAllWords().take(10)
        }
    }
}
