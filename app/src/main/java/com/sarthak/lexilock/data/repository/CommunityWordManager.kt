package com.sarthak.lexilock.data.repository

import com.sarthak.lexilock.domain.model.WordItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CommunityWord(
    val wordItem: WordItem,
    val etymologyFact: String,
    val dailyCuratorNote: String,
    val formattedDate: String
)

object CommunityWordManager {

    private val ETYMOLOGY_NOTES = mapOf(
        "eloquent" to "From Latin 'eloqui' (to speak out). It historically denoted the ability to sway civic assemblies through balanced cadence.",
        "ubiquitous" to "Derived from the Latin 'ubique' meaning 'everywhere'. Popularized in 17th-century theological debates.",
        "ephemeral" to "From Greek 'ephēmeros' (lasting only one day). Originally a medical term denoting a fever that subsided within 24 hours.",
        "resilience" to "From Latin 'resilire' (to leap back or rebound). Adopted by 19th-century mechanical engineers before entering psychology.",
        "pragmatic" to "Originates from Greek 'pragma' (deed, act). Grounded in practical action rather than abstract contemplation.",
        "concur" to "From Latin 'concurrere' (to run together). Signifies multiple minds arriving at the exact same conclusion simultaneously.",
        "foster" to "Old English 'fōstrian' (to nourish). Originally meant providing food and parental care to develop potential."
    )

    fun getTodayCommunityWord(repository: WordRepository): CommunityWord {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(cal.time)

        val allWords = repository.getAllWords()
        val index = (year * 365 + dayOfYear * 13) % (allWords.size.coerceAtLeast(1))
        val word = allWords[index.coerceIn(0, allWords.lastIndex)]

        val note = ETYMOLOGY_NOTES[word.word.lowercase()]
            ?: "Roots trace through classical rhetoric, describing an essential nuance in ${word.category.lowercase()} discourse."

        return CommunityWord(
            wordItem = word,
            etymologyFact = note,
            dailyCuratorNote = "Featured in today's global editorial study set.",
            formattedDate = dateStr
        )
    }
}
