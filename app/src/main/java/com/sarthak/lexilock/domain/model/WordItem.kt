package com.sarthak.lexilock.domain.model

data class WordItem(
    val id: Long,
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val exampleSentence: String,
    val pronunciation: String,
    val difficulty: String, // "beginner", "intermediate", "advanced", "expert"
    val synonyms: List<String>,
    val antonyms: List<String>,
    val category: String, // "Everyday English", "Academic", "Business", "Technology", "Emotions", "Personality", "Communication", "Interviews"
    val isSaved: Boolean = false,
    val isMastered: Boolean = false,
    val timesViewed: Int = 0,
    val lastShownDate: String = "" // "YYYY-MM-DD"
)
