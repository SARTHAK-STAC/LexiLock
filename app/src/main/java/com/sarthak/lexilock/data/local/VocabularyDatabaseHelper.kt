package com.sarthak.lexilock.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sarthak.lexilock.domain.model.WordItem

class VocabularyDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "lexilock_vocabulary.db"
        const val DATABASE_VERSION = 1

        const val TABLE_WORDS = "words"
        const val COL_ID = "id"
        const val COL_WORD = "word"
        const val COL_PART_OF_SPEECH = "part_of_speech"
        const val COL_DEFINITION = "definition"
        const val COL_EXAMPLE_SENTENCE = "example_sentence"
        const val COL_PRONUNCIATION = "pronunciation"
        const val COL_DIFFICULTY = "difficulty"
        const val COL_SYNONYMS = "synonyms"
        const val COL_ANTONYMS = "antonyms"
        const val COL_CATEGORY = "category"
        const val COL_IS_SAVED = "is_saved"
        const val COL_IS_MASTERED = "is_mastered"
        const val COL_TIMES_VIEWED = "times_viewed"
        const val COL_LAST_SHOWN_DATE = "last_shown_date"

        const val TABLE_LEARNING_LOG = "learning_log"
        const val COL_LOG_DATE = "log_date"
        const val COL_LOG_COUNT = "learned_count"

        @Volatile
        private var INSTANCE: VocabularyDatabaseHelper? = null

        fun getInstance(context: Context): VocabularyDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VocabularyDatabaseHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createWordsTable = """
            CREATE TABLE $TABLE_WORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WORD TEXT UNIQUE NOT NULL,
                $COL_PART_OF_SPEECH TEXT NOT NULL,
                $COL_DEFINITION TEXT NOT NULL,
                $COL_EXAMPLE_SENTENCE TEXT NOT NULL,
                $COL_PRONUNCIATION TEXT NOT NULL,
                $COL_DIFFICULTY TEXT NOT NULL,
                $COL_SYNONYMS TEXT NOT NULL,
                $COL_ANTONYMS TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_IS_SAVED INTEGER DEFAULT 0,
                $COL_IS_MASTERED INTEGER DEFAULT 0,
                $COL_TIMES_VIEWED INTEGER DEFAULT 0,
                $COL_LAST_SHOWN_DATE TEXT DEFAULT ''
            );
        """.trimIndent()

        val createLogTable = """
            CREATE TABLE $TABLE_LEARNING_LOG (
                $COL_LOG_DATE TEXT PRIMARY KEY NOT NULL,
                $COL_LOG_COUNT INTEGER DEFAULT 0
            );
        """.trimIndent()

        db.execSQL(createWordsTable)
        db.execSQL(createLogTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNING_LOG")
        onCreate(db)
    }

    fun cursorToWordItem(cursor: Cursor): WordItem {
        val synonymsRaw = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNONYMS))
        val antonymsRaw = cursor.getString(cursor.getColumnIndexOrThrow(COL_ANTONYMS))

        return WordItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            word = cursor.getString(cursor.getColumnIndexOrThrow(COL_WORD)),
            partOfSpeech = cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_OF_SPEECH)),
            definition = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEFINITION)),
            exampleSentence = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXAMPLE_SENTENCE)),
            pronunciation = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRONUNCIATION)),
            difficulty = cursor.getString(cursor.getColumnIndexOrThrow(COL_DIFFICULTY)),
            synonyms = if (synonymsRaw.isBlank()) emptyList() else synonymsRaw.split("|").map { it.trim() },
            antonyms = if (antonymsRaw.isBlank()) emptyList() else antonymsRaw.split("|").map { it.trim() },
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            isSaved = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SAVED)) == 1,
            isMastered = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_MASTERED)) == 1,
            timesViewed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TIMES_VIEWED)),
            lastShownDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_SHOWN_DATE)) ?: ""
        )
    }
}
