package com.sarthak.lexilock.data.repository

import android.content.ContentValues
import android.content.Context
import com.sarthak.lexilock.data.local.StarterVocabulary
import com.sarthak.lexilock.data.local.VocabularyDatabaseHelper
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.data.util.LexiTimeProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WordRepository(val context: Context) {

    private val dbHelper = VocabularyDatabaseHelper.getInstance(context)
    private val prefs = LexiPreferences.getInstance(context)

    init {
        ensureDatabasePopulated()
    }

    private fun ensureDatabasePopulated() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS}", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()

        if (count == 0) {
            val writeDb = dbHelper.writableDatabase
            writeDb.beginTransaction()
            try {
                for (item in StarterVocabulary.dataset) {
                    val values = ContentValues().apply {
                        put(VocabularyDatabaseHelper.COL_WORD, item.word)
                        put(VocabularyDatabaseHelper.COL_PART_OF_SPEECH, item.partOfSpeech)
                        put(VocabularyDatabaseHelper.COL_DEFINITION, item.definition)
                        put(VocabularyDatabaseHelper.COL_EXAMPLE_SENTENCE, item.exampleSentence)
                        put(VocabularyDatabaseHelper.COL_PRONUNCIATION, item.pronunciation)
                        put(VocabularyDatabaseHelper.COL_DIFFICULTY, item.difficulty)
                        put(VocabularyDatabaseHelper.COL_SYNONYMS, item.synonyms.joinToString("|"))
                        put(VocabularyDatabaseHelper.COL_ANTONYMS, item.antonyms.joinToString("|"))
                        put(VocabularyDatabaseHelper.COL_CATEGORY, item.category)
                        put(VocabularyDatabaseHelper.COL_IS_SAVED, if (item.isSaved) 1 else 0)
                        put(VocabularyDatabaseHelper.COL_IS_MASTERED, if (item.isMastered) 1 else 0)
                        put(VocabularyDatabaseHelper.COL_TIMES_VIEWED, item.timesViewed)
                        put(VocabularyDatabaseHelper.COL_LAST_SHOWN_DATE, item.lastShownDate)
                    }
                    writeDb.insertWithOnConflict(
                        VocabularyDatabaseHelper.TABLE_WORDS,
                        null,
                        values,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
                    )
                }
                writeDb.setTransactionSuccessful()
            } finally {
                writeDb.endTransaction()
            }
        }
    }

    fun getTodayDateString(): String {
        return LexiTimeProvider.getTodayDateString()
    }

    fun getTodayWords(): List<WordItem> {
        val count = prefs.wordsPerDay.coerceIn(1, 5)
        val todayStr = getTodayDateString()
        val level = prefs.englishLevel.lowercase()

        val cachedIds = prefs.todayWordIds
        if (prefs.todayDate == todayStr && prefs.cachedLevel == level && cachedIds.size == count) {
            val cachedWords = cachedIds.mapNotNull { getWordById(it) }
            if (cachedWords.size == count) {
                return cachedWords
            }
        }

        // Generate deterministically for today, count, and level
        val selectedWords = selectWordsForDate(todayStr, count, level)
        prefs.todayDate = todayStr
        prefs.cachedLevel = level
        prefs.todayWordIds = selectedWords.map { it.id }
        if (selectedWords.isNotEmpty()) {
            prefs.todayWordId = selectedWords.first().id
        }

        for (w in selectedWords) {
            incrementViewsAndShownDate(w.id, todayStr)
        }
        return selectedWords
    }

    fun getTodayWord(): WordItem {
        return getTodayWords().firstOrNull() ?: getAllWords().firstOrNull() ?: StarterVocabulary.dataset.first()
    }

    private fun selectWordsForDate(dateStr: String, count: Int, level: String): List<WordItem> {
        val db = dbHelper.readableDatabase

        val targetDifficulties = when (level) {
            "beginner" -> listOf("beginner", "intermediate")
            "intermediate" -> listOf("intermediate", "beginner", "advanced")
            "advanced" -> listOf("advanced", "expert", "intermediate")
            "expert" -> listOf("expert", "advanced")
            else -> listOf("intermediate", "advanced")
        }

        val placeholders = targetDifficulties.joinToString(",") { "?" }
        val query = """
            SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS}
            WHERE ${VocabularyDatabaseHelper.COL_DIFFICULTY} IN ($placeholders)
            AND ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 0
            ORDER BY ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} ASC, ${VocabularyDatabaseHelper.COL_ID} ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, targetDifficulties.toTypedArray())
        val candidates = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            candidates.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()

        // Fallback to other unmastered words if not enough
        if (candidates.size < count) {
            val allUnmasteredQuery = """
                SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS}
                WHERE ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 0
                ORDER BY ${VocabularyDatabaseHelper.COL_ID} ASC
            """.trimIndent()
            val c2 = db.rawQuery(allUnmasteredQuery, null)
            while (c2.moveToNext()) {
                val item = dbHelper.cursorToWordItem(c2)
                if (candidates.none { it.id == item.id }) {
                    candidates.add(item)
                }
            }
            c2.close()
        }

        // If all words are mastered, use full library
        if (candidates.size < count) {
            val allWords = getAllWords()
            for (item in allWords) {
                if (candidates.none { it.id == item.id }) {
                    candidates.add(item)
                }
            }
        }

        if (candidates.isEmpty()) {
            return StarterVocabulary.dataset.take(count)
        }

        val result = mutableListOf<WordItem>()
        val available = candidates.toMutableList()

        for (i in 0 until count) {
            if (available.isEmpty()) break
            val slotHash = Math.abs((dateStr + "_" + level + "_" + i).hashCode())
            val index = slotHash % available.size
            result.add(available.removeAt(index))
        }

        return result
    }

    private fun incrementViewsAndShownDate(id: Long, dateStr: String) {
        val db = dbHelper.writableDatabase
        db.execSQL(
            "UPDATE ${VocabularyDatabaseHelper.TABLE_WORDS} SET ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} = ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} + 1, ${VocabularyDatabaseHelper.COL_LAST_SHOWN_DATE} = ? WHERE ${VocabularyDatabaseHelper.COL_ID} = ?",
            arrayOf(dateStr, id.toString())
        )
    }

    fun getWordById(id: Long): WordItem? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        var result: WordItem? = null
        if (cursor.moveToFirst()) {
            result = dbHelper.cursorToWordItem(cursor)
        }
        cursor.close()
        return result
    }

    fun getAllWords(): List<WordItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} ORDER BY ${VocabularyDatabaseHelper.COL_WORD} ASC", null)
        val list = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            list.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()
        return list
    }

    fun searchWords(
        query: String,
        difficulty: String? = null,
        category: String? = null
    ): List<WordItem> {
        val db = dbHelper.readableDatabase
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (query.isNotBlank()) {
            selectionParts.add("(${VocabularyDatabaseHelper.COL_WORD} LIKE ? OR ${VocabularyDatabaseHelper.COL_DEFINITION} LIKE ?)")
            val pattern = "%${query.trim()}%"
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
        }

        if (!difficulty.isNullOrBlank() && difficulty != "All") {
            selectionParts.add("${VocabularyDatabaseHelper.COL_DIFFICULTY} = ?")
            selectionArgs.add(difficulty.lowercase())
        }

        if (!category.isNullOrBlank() && category != "All") {
            selectionParts.add("${VocabularyDatabaseHelper.COL_CATEGORY} = ?")
            selectionArgs.add(category)
        }

        val whereClause = if (selectionParts.isEmpty()) "" else "WHERE " + selectionParts.joinToString(" AND ")
        val sql = "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} $whereClause ORDER BY ${VocabularyDatabaseHelper.COL_WORD} ASC"

        val cursor = db.rawQuery(sql, if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray())
        val list = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            list.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()
        return list
    }

    fun getWordsByCategory(category: String): List<WordItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_CATEGORY} = ? ORDER BY ${VocabularyDatabaseHelper.COL_WORD} ASC",
            arrayOf(category)
        )
        val list = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            list.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()
        return list
    }

    fun getLearnedWordsCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} > 0 OR ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 1",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return maxOf(count, prefs.totalWordsLearned)
    }

    fun getSavedWords(): List<WordItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_IS_SAVED} = 1 ORDER BY ${VocabularyDatabaseHelper.COL_WORD} ASC",
            null
        )
        val list = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            list.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()
        return list
    }

    fun getReviewWords(limit: Int = 10): List<WordItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_IS_SAVED} = 1 OR (${VocabularyDatabaseHelper.COL_TIMES_VIEWED} > 0 AND ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 0) ORDER BY ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} ASC, ${VocabularyDatabaseHelper.COL_IS_SAVED} DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        val list = mutableListOf<WordItem>()
        while (cursor.moveToNext()) {
            list.add(dbHelper.cursorToWordItem(cursor))
        }
        cursor.close()
        if (list.size < 5) {
            val all = getAllWords().shuffled().take(limit)
            return (list + all).distinctBy { it.id }.take(limit)
        }
        return list
    }

    fun toggleSave(id: Long, isSaved: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(VocabularyDatabaseHelper.COL_IS_SAVED, if (isSaved) 1 else 0)
        }
        db.update(VocabularyDatabaseHelper.TABLE_WORDS, values, "${VocabularyDatabaseHelper.COL_ID} = ?", arrayOf(id.toString()))
    }

    fun markAsMastered(id: Long, isMastered: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(VocabularyDatabaseHelper.COL_IS_MASTERED, if (isMastered) 1 else 0)
        }
        db.update(VocabularyDatabaseHelper.TABLE_WORDS, values, "${VocabularyDatabaseHelper.COL_ID} = ?", arrayOf(id.toString()))

        if (isMastered) {
            recordWordLearnedToday()
        }
    }

    fun recordGameWordCorrect(wordId: Long) {
        val todayStr = getTodayDateString()
        val db = dbHelper.writableDatabase
        db.execSQL(
            "UPDATE ${VocabularyDatabaseHelper.TABLE_WORDS} SET ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} = ${VocabularyDatabaseHelper.COL_TIMES_VIEWED} + 1, ${VocabularyDatabaseHelper.COL_LAST_SHOWN_DATE} = ? WHERE ${VocabularyDatabaseHelper.COL_ID} = ?",
            arrayOf(todayStr, wordId.toString())
        )
    }

    fun clearAllSavedWords() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(VocabularyDatabaseHelper.COL_IS_SAVED, 0)
        }
        db.update(VocabularyDatabaseHelper.TABLE_WORDS, values, null, null)
    }

    fun resetAllLearningProgress() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(VocabularyDatabaseHelper.COL_IS_MASTERED, 0)
            put(VocabularyDatabaseHelper.COL_TIMES_VIEWED, 0)
            put(VocabularyDatabaseHelper.COL_LAST_SHOWN_DATE, "")
        }
        db.update(VocabularyDatabaseHelper.TABLE_WORDS, values, null, null)
        db.execSQL("DELETE FROM ${VocabularyDatabaseHelper.TABLE_LEARNING_LOG}")
        prefs.resetProgress()
    }

    // ==========================================
    // STREAK & PROGRESS SYSTEM (Timezone-safe)
    // ==========================================

    private fun recordWordLearnedToday() {
        val todayStr = getTodayDateString()
        val db = dbHelper.writableDatabase
        db.execSQL(
            "INSERT INTO ${VocabularyDatabaseHelper.TABLE_LEARNING_LOG} (${VocabularyDatabaseHelper.COL_LOG_DATE}, ${VocabularyDatabaseHelper.COL_LOG_COUNT}) VALUES (?, 1) ON CONFLICT(${VocabularyDatabaseHelper.COL_LOG_DATE}) DO UPDATE SET ${VocabularyDatabaseHelper.COL_LOG_COUNT} = ${VocabularyDatabaseHelper.COL_LOG_COUNT} + 1",
            arrayOf(todayStr)
        )

        updateStreakOnLearning(todayStr)
    }

    fun updateStreakOnLearning(todayStr: String = getTodayDateString()) {
        val lastDate = prefs.lastLearnedDate
        if (lastDate == todayStr) {
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance().apply {
            time = sdf.parse(todayStr) ?: Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var newStreak = 1
        if (lastDate.isNotBlank()) {
            val lastCal = Calendar.getInstance().apply {
                time = sdf.parse(lastDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMillis = todayCal.timeInMillis - lastCal.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            newStreak = when (diffDays) {
                1 -> prefs.currentStreak + 1 // Consecutive day
                0 -> prefs.currentStreak     // Same day
                else -> 1                    // Broken streak
            }
        }

        prefs.currentStreak = newStreak
        if (newStreak > prefs.longestStreak) {
            prefs.longestStreak = newStreak
        }
        prefs.lastLearnedDate = todayStr
        prefs.totalWordsLearned = prefs.totalWordsLearned + 1
    }

    fun getMasteredWordsCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 1", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getWeeklyActivity(): Map<String, Int> {
        val db = dbHelper.readableDatabase
        val result = mutableMapOf<String, Int>()
        val dayKeys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        dayKeys.forEach { result[it] = 0 }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (i in 0 until 7) {
            val dateStr = sdf.format(cal.time)
            val cursor = db.rawQuery(
                "SELECT ${VocabularyDatabaseHelper.COL_LOG_COUNT} FROM ${VocabularyDatabaseHelper.TABLE_LEARNING_LOG} WHERE ${VocabularyDatabaseHelper.COL_LOG_DATE} = ?",
                arrayOf(dateStr)
            )
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            result[dayKeys[i]] = count
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return result
    }

    fun getProgressStats(): ProgressStats {
        val db = dbHelper.readableDatabase
        val totalCursor = db.rawQuery("SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS}", null)
        val totalCount = if (totalCursor.moveToFirst()) totalCursor.getInt(0) else 200
        totalCursor.close()

        val masteredCursor = db.rawQuery("SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_IS_MASTERED} = 1", null)
        val masteredCount = if (masteredCursor.moveToFirst()) masteredCursor.getInt(0) else 0
        masteredCursor.close()

        val savedCursor = db.rawQuery("SELECT COUNT(*) FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE ${VocabularyDatabaseHelper.COL_IS_SAVED} = 1", null)
        val savedCount = if (savedCursor.moveToFirst()) savedCursor.getInt(0) else 0
        savedCursor.close()

        return ProgressStats(
            totalWords = totalCount,
            masteredWords = masteredCount,
            savedWords = savedCount,
            streakDays = prefs.currentStreak,
            longestStreak = prefs.longestStreak
        )
    }

    fun findWordByExactText(wordText: String): WordItem? {
        if (wordText.isBlank()) return null
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${VocabularyDatabaseHelper.TABLE_WORDS} WHERE LOWER(${VocabularyDatabaseHelper.COL_WORD}) = LOWER(?) LIMIT 1",
            arrayOf(wordText.trim())
        )
        var item: WordItem? = null
        if (cursor.moveToFirst()) {
            item = dbHelper.cursorToWordItem(cursor)
        }
        cursor.close()
        return item
    }

    fun getDailyWordSchedule(): List<ScheduledWordSlot> {
        val words = getTodayWords()
        if (words.isEmpty()) return emptyList()
        val count = words.size
        val cal = LexiTimeProvider.getCalendar()
        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val curMin = cal.get(Calendar.MINUTE)
        val curSec = cal.get(Calendar.SECOND)
        val curMs = cal.get(Calendar.MILLISECOND)

        val curMillisOfDay = ((curHour * 60 + curMin) * 60 + curSec) * 1000L + curMs
        val totalDayMillis = 24 * 60 * 60 * 1000L
        val currentSlotIndex = ((curMillisOfDay * count) / totalDayMillis).toInt().coerceIn(0, count - 1)

        return words.mapIndexed { index, word ->
            val startMinTotal = (index * 1440) / count
            val endMinTotal = ((index + 1) * 1440) / count

            val startHour = startMinTotal / 60
            val startMin = startMinTotal % 60
            val endHour = endMinTotal / 60
            val endMin = endMinTotal % 60

            val startTimeFormatted = String.format(Locale.US, "%02d:%02d", startHour, startMin)
            val endTimeFormatted = if (endHour == 24 && endMin == 0) "24:00" else String.format(Locale.US, "%02d:%02d", endHour, endMin)
            val timeRangeText = "$startTimeFormatted — $endTimeFormatted"

            val isCurrent = (index == currentSlotIndex)
            val slotEndMillis = ((index + 1) * totalDayMillis) / count
            val remainingMillis = if (isCurrent) (slotEndMillis - curMillisOfDay).coerceAtLeast(0L) else 0L

            val remSeconds = remainingMillis / 1000L
            val remHours = remSeconds / 3600L
            val remMinutes = (remSeconds % 3600L) / 60L
            val countdownText = if (count == 1) {
                "All day"
            } else {
                String.format(Locale.US, "Next word in: %02dh %02dm", remHours, remMinutes)
            }

            ScheduledWordSlot(
                index = index,
                word = word,
                startHour = startHour,
                startMinute = startMin,
                endHour = endHour,
                endMinute = endMin,
                startTimeFormatted = startTimeFormatted,
                endTimeFormatted = endTimeFormatted,
                timeRangeText = timeRangeText,
                isCurrent = isCurrent,
                remainingMillisInSlot = remainingMillis,
                countdownText = countdownText
            )
        }
    }

    fun getCurrentScheduledWord(): WordItem {
        val words = getTodayWords()
        if (words.isEmpty()) return getTodayWord()
        val count = words.size
        val cal = LexiTimeProvider.getCalendar()
        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val curMin = cal.get(Calendar.MINUTE)
        val curSec = cal.get(Calendar.SECOND)
        val curMs = cal.get(Calendar.MILLISECOND)

        val curMillisOfDay = ((curHour * 60 + curMin) * 60 + curSec) * 1000L + curMs
        val totalDayMillis = 24 * 60 * 60 * 1000L
        val currentSlotIndex = ((curMillisOfDay * count) / totalDayMillis).toInt().coerceIn(0, count - 1)
        return words.getOrNull(currentSlotIndex) ?: words.first()
    }

    fun getRemainingMillisToNextSlot(): Long {
        val words = getTodayWords()
        val count = if (words.isNotEmpty()) words.size else prefs.wordsPerDay.coerceIn(1, 5)
        val cal = LexiTimeProvider.getCalendar()
        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val curMin = cal.get(Calendar.MINUTE)
        val curSec = cal.get(Calendar.SECOND)
        val curMs = cal.get(Calendar.MILLISECOND)

        val curMillisOfDay = ((curHour * 60 + curMin) * 60 + curSec) * 1000L + curMs
        val totalDayMillis = 24 * 60 * 60 * 1000L
        val currentSlotIndex = ((curMillisOfDay * count) / totalDayMillis).toInt().coerceIn(0, count - 1)
        val slotEndMillis = ((currentSlotIndex + 1) * totalDayMillis) / count
        return (slotEndMillis - curMillisOfDay).coerceAtLeast(1000L)
    }

    fun getActiveWallpaperWord(): WordItem {
        if (prefs.wallpaperWordMode == "manual") {
            val manualWord = prefs.manualWallpaperWord
            if (manualWord.isNotBlank()) {
                val found = findWordByExactText(manualWord)
                if (found != null) {
                    return found
                }
            }
        }
        return getCurrentScheduledWord()
    }
}

data class ScheduledWordSlot(
    val index: Int,
    val word: WordItem,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val timeRangeText: String,
    val isCurrent: Boolean,
    val remainingMillisInSlot: Long,
    val countdownText: String
)

data class ProgressStats(
    val totalWords: Int,
    val masteredWords: Int,
    val savedWords: Int,
    val streakDays: Int,
    val longestStreak: Int
)
