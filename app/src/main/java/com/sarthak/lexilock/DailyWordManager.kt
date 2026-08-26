package com.sarthak.lexilock

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailyWordManager {

    fun getTodayWord(context: Context): Word {

        val level = UserPreferences.getLevel(context)

        val levelWords = WordRepository.words.filter {
            it.level == level
        }

        if (levelWords.isEmpty()) {
            return WordRepository.words[0]
        }

        val today = SimpleDateFormat(
            "yyyyMMdd",
            Locale.US
        ).format(Date())

        val dayNumber = today.toLong()

        val index =
            (dayNumber % levelWords.size).toInt()

        return levelWords[index]
    }
}