package com.sarthak.lexilock.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.sarthak.lexilock.data.model.CustomWallpaperPreset
import com.sarthak.lexilock.data.model.WallpaperConfig
import com.sarthak.lexilock.data.model.WallpaperStyle
import org.json.JSONArray

class LexiPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_FILE_NAME = "lexilock_settings_prefs"

        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_ENGLISH_LEVEL = "key_english_level"
        private const val KEY_LEARNING_GOALS = "key_learning_goals"
        private const val KEY_WORDS_PER_DAY = "key_words_per_day"

        // Appearance & Wallpaper
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_WALLPAPER_STYLE = "key_wallpaper_style"
        private const val KEY_WALLPAPER_CONFIG_JSON = "key_wallpaper_config_json"
        private const val KEY_WALLPAPER_PRESETS_JSON = "key_wallpaper_presets_json"
        private const val KEY_WALLPAPER_WORD_MODE = "key_wallpaper_word_mode" // "daily" or "manual"
        private const val KEY_MANUAL_WALLPAPER_WORD = "key_manual_wallpaper_word"

        // Lock screen settings
        private const val KEY_LOCKSCREEN_ENABLED = "key_lockscreen_enabled"
        private const val KEY_SHOW_DAILY_WORD = "key_show_daily_word"
        private const val KEY_SHOW_DEFINITION = "key_show_definition"
        private const val KEY_SHOW_EXAMPLE = "key_show_example"
        private const val KEY_SHOW_PRONUNCIATION = "key_show_pronunciation"
        private const val KEY_SHOW_DIFFICULTY = "key_show_difficulty"
        private const val KEY_SHOW_STREAK = "key_show_streak"

        // Notifications
        private const val KEY_NOTIF_ENABLED = "key_notif_enabled"
        private const val KEY_NOTIF_HOUR = "key_notif_hour"
        private const val KEY_NOTIF_MINUTE = "key_notif_minute"

        // Streak & Stats
        private const val KEY_CURRENT_STREAK = "key_current_streak"
        private const val KEY_LONGEST_STREAK = "key_longest_streak"
        private const val KEY_LAST_LEARNED_DATE = "key_last_learned_date"
        private const val KEY_TOTAL_WORDS_LEARNED = "key_total_words_learned"

        // XP & Rewards
        private const val KEY_USER_XP = "key_user_xp"
        private const val KEY_UNLOCKED_BADGES_JSON = "key_unlocked_badges_json"
        private const val KEY_COMPLETED_CHALLENGES_COUNT = "key_completed_challenges_count"
        private const val KEY_TTS_SESSIONS_COUNT = "key_tts_sessions_count"

        // Daily Challenge Cache
        private const val KEY_CHALLENGE_DATE = "key_challenge_date"
        private const val KEY_CHALLENGE_JSON = "key_challenge_json"
        private const val KEY_CHALLENGE_PROGRESS_COUNT = "key_challenge_progress_count"
        private const val KEY_CHALLENGE_COMPLETED = "key_challenge_completed"

        // Today's Word cache
        private const val KEY_TODAY_WORD_ID = "key_today_word_id"
        private const val KEY_TODAY_WORD_IDS = "key_today_word_ids"
        private const val KEY_TODAY_DATE = "key_today_date"
        private const val KEY_CACHED_LEVEL = "key_cached_level"

        // Arcade & Mini-Games
        private const val KEY_ARCADE_GAMES_PLAYED = "key_arcade_games_played"
        private const val KEY_ARCADE_GAME_STREAK = "key_arcade_game_streak"
        private const val KEY_ARCADE_LAST_PLAYED_DATE = "key_arcade_last_played_date"
        private const val KEY_ARCADE_FIRST_PLAY_BONUS_DATE = "key_arcade_first_play_bonus_date"
        private const val KEY_ARCADE_BEST_SCORES_JSON = "key_arcade_best_scores_json"

        // Phase 2: Daily Challenge & Mystery Word
        private const val KEY_DAILY_CHALLENGE_DATE = "key_daily_challenge_date"
        private const val KEY_DAILY_CHALLENGE_COMPLETED = "key_daily_challenge_completed"
        private const val KEY_DAILY_CHALLENGE_SCORE = "key_daily_challenge_score"
        private const val KEY_MYSTERY_DATE = "key_mystery_date"
        private const val KEY_MYSTERY_WORD_ID = "key_mystery_word_id"
        private const val KEY_MYSTERY_CLUES_REVEALED = "key_mystery_clues_revealed"
        private const val KEY_MYSTERY_SOLVED = "key_mystery_solved"
        private const val KEY_MYSTERY_SOLVED_COUNT = "key_mystery_solved_count"

        // Phase 2: Achievements, League, Events
        private const val KEY_UNLOCKED_ACHIEVEMENTS = "key_unlocked_achievements"
        private const val KEY_PERFECT_GAMES_COUNT = "key_perfect_games_count"
        private const val KEY_LEAGUE_WEEK_KEY = "key_league_week_key"
        private const val KEY_LEAGUE_XP = "key_league_xp"
        private const val KEY_COMPLETED_EVENTS = "key_completed_events"
        private const val KEY_PRONUNCIATION_COMPLETED_COUNT = "key_pronunciation_completed_count"

        // Phase 3: Viral, Progression & Polish
        private const val KEY_ARCADE_TOTAL_XP = "key_arcade_total_xp"
        private const val KEY_SELECTED_PLAYER_TITLE = "key_selected_player_title"
        private const val KEY_SOUND_EFFECTS_ENABLED = "key_sound_effects_enabled"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"

        @Volatile
        private var INSTANCE: LexiPreferences? = null

        fun getInstance(context: Context): LexiPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LexiPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var englishLevel: String
        get() = prefs.getString(KEY_ENGLISH_LEVEL, "intermediate") ?: "intermediate"
        set(value) = prefs.edit().putString(KEY_ENGLISH_LEVEL, value).apply()

    var learningGoals: Set<String>
        get() = prefs.getStringSet(KEY_LEARNING_GOALS, setOf("Improve everyday English")) ?: setOf("Improve everyday English")
        set(value) = prefs.edit().putStringSet(KEY_LEARNING_GOALS, value).apply()

    var wordsPerDay: Int
        get() = prefs.getInt(KEY_WORDS_PER_DAY, 1)
        set(value) = prefs.edit().putInt(KEY_WORDS_PER_DAY, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var wallpaperStyle: String
        get() = prefs.getString(KEY_WALLPAPER_STYLE, "obsidian") ?: "obsidian"
        set(value) = prefs.edit().putString(KEY_WALLPAPER_STYLE, value).apply()

    var wallpaperWordMode: String
        get() = prefs.getString(KEY_WALLPAPER_WORD_MODE, "daily") ?: "daily"
        set(value) = prefs.edit().putString(KEY_WALLPAPER_WORD_MODE, value).apply()

    var manualWallpaperWord: String
        get() = prefs.getString(KEY_MANUAL_WALLPAPER_WORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MANUAL_WALLPAPER_WORD, value).apply()

    // Wallpaper Configuration
    var wallpaperConfig: WallpaperConfig
        get() {
            val raw = prefs.getString(KEY_WALLPAPER_CONFIG_JSON, "") ?: ""
            if (raw.isBlank()) {
                val currentStyle = WallpaperStyle.fromId(wallpaperStyle)
                return WallpaperConfig(
                    style = currentStyle,
                    showWord = showDailyWordOnLock,
                    showIpa = showPronunciationOnLock,
                    showDefinition = showDefinitionOnLock,
                    showExample = showExampleOnLock,
                    showStreak = showStreakOnLock
                )
            }
            return WallpaperConfig.fromJson(raw)
        }
        set(value) {
            prefs.edit().putString(KEY_WALLPAPER_CONFIG_JSON, value.toJson()).apply()
            wallpaperStyle = value.style.id
            showDailyWordOnLock = value.showWord
            showPronunciationOnLock = value.showIpa
            showDefinitionOnLock = value.showDefinition
            showExampleOnLock = value.showExample
            showStreakOnLock = value.showStreak
        }

    // Saved Wallpaper Presets
    var customWallpaperPresets: List<CustomWallpaperPreset>
        get() {
            val raw = prefs.getString(KEY_WALLPAPER_PRESETS_JSON, "") ?: ""
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val list = mutableListOf<CustomWallpaperPreset>()
                for (i in 0 until arr.length()) {
                    CustomWallpaperPreset.fromJson(arr.getString(i))?.let { list.add(it) }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { arr.put(it.toJson()) }
            prefs.edit().putString(KEY_WALLPAPER_PRESETS_JSON, arr.toString()).apply()
        }

    fun savePreset(preset: CustomWallpaperPreset) {
        val current = customWallpaperPresets.toMutableList()
        current.removeAll { it.id == preset.id }
        current.add(0, preset)
        customWallpaperPresets = current
    }

    fun deletePreset(presetId: String) {
        val current = customWallpaperPresets.toMutableList()
        current.removeAll { it.id == presetId }
        customWallpaperPresets = current
    }

    // ==========================================
    // PER-WORD WALLPAPER CUSTOMIZATION
    // Key pattern: "word_wp_config_<word_lowercase>"
    // ==========================================
    private fun getWordPrefKey(word: String): String = "word_wp_config_${word.trim().lowercase()}"

    fun hasWordConfig(word: String): Boolean {
        if (word.isBlank()) return false
        return prefs.contains(getWordPrefKey(word))
    }

    fun getWordConfig(word: String): WallpaperConfig {
        if (word.isBlank()) return wallpaperConfig
        val raw = prefs.getString(getWordPrefKey(word), null)
        return if (!raw.isNullOrBlank()) {
            WallpaperConfig.fromJson(raw)
        } else {
            wallpaperConfig
        }
    }

    fun setWordConfig(word: String, config: WallpaperConfig) {
        if (word.isBlank()) return
        prefs.edit().putString(getWordPrefKey(word), config.toJson()).apply()
    }

    fun resetWordConfig(word: String) {
        if (word.isBlank()) return
        prefs.edit().remove(getWordPrefKey(word)).apply()
    }

    fun resetAllWordCustomizations() {
        val allKeys = prefs.all.keys
        val editor = prefs.edit()
        for (key in allKeys) {
            if (key.startsWith("word_wp_config_")) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun applyPresetToWord(word: String, presetConfig: WallpaperConfig) {
        setWordConfig(word, presetConfig)
    }

    fun applyPresetToWords(words: List<String>, presetConfig: WallpaperConfig) {
        val editor = prefs.edit()
        for (w in words) {
            if (w.isNotBlank()) {
                editor.putString(getWordPrefKey(w), presetConfig.toJson())
            }
        }
        editor.apply()
    }

    fun applyPresetToAll(presetConfig: WallpaperConfig) {
        wallpaperConfig = presetConfig
    }

    var isLockscreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKSCREEN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_ENABLED, value).apply()

    var showDailyWordOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DAILY_WORD, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DAILY_WORD, value).apply()

    var showDefinitionOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DEFINITION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DEFINITION, value).apply()

    var showExampleOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EXAMPLE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_EXAMPLE, value).apply()

    var showPronunciationOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PRONUNCIATION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PRONUNCIATION, value).apply()

    var showDifficultyOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DIFFICULTY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DIFFICULTY, value).apply()

    var showStreakOnLock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_STREAK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_STREAK, value).apply()

    var isDailyNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    var notificationHour: Int
        get() = prefs.getInt(KEY_NOTIF_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_NOTIF_HOUR, value).apply()

    var notificationMinute: Int
        get() = prefs.getInt(KEY_NOTIF_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_NOTIF_MINUTE, value).apply()

    var currentStreak: Int
        get() = prefs.getInt(KEY_CURRENT_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_STREAK, value).apply()

    var longestStreak: Int
        get() = prefs.getInt(KEY_LONGEST_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_LONGEST_STREAK, value).apply()

    var lastLearnedDate: String
        get() = prefs.getString(KEY_LAST_LEARNED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LEARNED_DATE, value).apply()

    var totalWordsLearned: Int
        get() = prefs.getInt(KEY_TOTAL_WORDS_LEARNED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_WORDS_LEARNED, value).apply()

    // XP & Rewards
    var userXp: Int
        get() = prefs.getInt(KEY_USER_XP, 0)
        set(value) = prefs.edit().putInt(KEY_USER_XP, value).apply()

    fun addXp(amount: Int): Int {
        val newXp = (userXp + amount).coerceAtLeast(0)
        userXp = newXp
        return newXp
    }

    var unlockedBadgesIds: Set<String>
        get() = prefs.getStringSet(KEY_UNLOCKED_BADGES_JSON, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_UNLOCKED_BADGES_JSON, value).apply()

    var completedChallengesCount: Int
        get() = prefs.getInt(KEY_COMPLETED_CHALLENGES_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_COMPLETED_CHALLENGES_COUNT, value).apply()

    var ttsSessionsCount: Int
        get() = prefs.getInt(KEY_TTS_SESSIONS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TTS_SESSIONS_COUNT, value).apply()

    // Daily Challenge
    var challengeDate: String
        get() = prefs.getString(KEY_CHALLENGE_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHALLENGE_DATE, value).apply()

    var challengeJson: String
        get() = prefs.getString(KEY_CHALLENGE_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHALLENGE_JSON, value).apply()

    var challengeProgressCount: Int
        get() = prefs.getInt(KEY_CHALLENGE_PROGRESS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_CHALLENGE_PROGRESS_COUNT, value).apply()

    var challengeCompleted: Boolean
        get() = prefs.getBoolean(KEY_CHALLENGE_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHALLENGE_COMPLETED, value).apply()

    // Today's Word cache
    var todayWordId: Long
        get() = prefs.getLong(KEY_TODAY_WORD_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_TODAY_WORD_ID, value).apply()

    var todayWordIds: List<Long>
        get() {
            val raw = prefs.getString(KEY_TODAY_WORD_IDS, "") ?: ""
            return if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
        set(value) {
            prefs.edit().putString(KEY_TODAY_WORD_IDS, value.joinToString(",")).apply()
        }

    var cachedLevel: String
        get() = prefs.getString(KEY_CACHED_LEVEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CACHED_LEVEL, value).apply()

    var todayDate: String
        get() = prefs.getString(KEY_TODAY_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TODAY_DATE, value).apply()

    // Arcade & Mini-Games
    var arcadeGamesPlayed: Int
        get() = prefs.getInt(KEY_ARCADE_GAMES_PLAYED, 0)
        set(value) = prefs.edit().putInt(KEY_ARCADE_GAMES_PLAYED, value).apply()

    var arcadeGameStreak: Int
        get() = prefs.getInt(KEY_ARCADE_GAME_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_ARCADE_GAME_STREAK, value).apply()

    var arcadeLastPlayedDate: String
        get() = prefs.getString(KEY_ARCADE_LAST_PLAYED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ARCADE_LAST_PLAYED_DATE, value).apply()

    var arcadeFirstPlayBonusDate: String
        get() = prefs.getString(KEY_ARCADE_FIRST_PLAY_BONUS_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ARCADE_FIRST_PLAY_BONUS_DATE, value).apply()

    fun getArcadeBestScore(gameId: String): Int {
        val raw = prefs.getString(KEY_ARCADE_BEST_SCORES_JSON, "") ?: ""
        if (raw.isBlank()) return 0
        return try {
            val json = org.json.JSONObject(raw)
            json.optInt(gameId, 0)
        } catch (e: Exception) {
            0
        }
    }

    fun setArcadeBestScore(gameId: String, score: Int) {
        val raw = prefs.getString(KEY_ARCADE_BEST_SCORES_JSON, "") ?: ""
        val json = try {
            if (raw.isNotBlank()) org.json.JSONObject(raw) else org.json.JSONObject()
        } catch (e: Exception) {
            org.json.JSONObject()
        }
        val currentBest = json.optInt(gameId, 0)
        if (score > currentBest) {
            json.put(gameId, score)
            prefs.edit().putString(KEY_ARCADE_BEST_SCORES_JSON, json.toString()).apply()
        }
    }

    fun getOverallBestScore(): Int {
        val raw = prefs.getString(KEY_ARCADE_BEST_SCORES_JSON, "") ?: ""
        if (raw.isBlank()) return 0
        return try {
            val json = org.json.JSONObject(raw)
            var maxScore = 0
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val sc = json.optInt(k, 0)
                if (sc > maxScore) maxScore = sc
            }
            maxScore
        } catch (e: Exception) {
            0
        }
    }

    // Phase 2: Daily Challenge
    fun isDailyChallengeCompletedToday(dateStr: String): Boolean {
        val lastDate = prefs.getString(KEY_DAILY_CHALLENGE_DATE, "") ?: ""
        return lastDate == dateStr && prefs.getBoolean(KEY_DAILY_CHALLENGE_COMPLETED, false)
    }

    fun markDailyChallengeCompleted(dateStr: String, score: Int) {
        prefs.edit()
            .putString(KEY_DAILY_CHALLENGE_DATE, dateStr)
            .putBoolean(KEY_DAILY_CHALLENGE_COMPLETED, true)
            .putInt(KEY_DAILY_CHALLENGE_SCORE, score)
            .apply()
    }

    fun getDailyChallengeScore(): Int {
        return prefs.getInt(KEY_DAILY_CHALLENGE_SCORE, 0)
    }

    // Phase 2: Mystery Word
    fun getMysteryWordState(dateStr: String): Triple<Long, Int, Boolean> {
        val lastDate = prefs.getString(KEY_MYSTERY_DATE, "") ?: ""
        return if (lastDate == dateStr) {
            val id = prefs.getLong(KEY_MYSTERY_WORD_ID, -1L)
            val clues = prefs.getInt(KEY_MYSTERY_CLUES_REVEALED, 1)
            val solved = prefs.getBoolean(KEY_MYSTERY_SOLVED, false)
            Triple(id, clues, solved)
        } else {
            Triple(-1L, 1, false)
        }
    }

    fun saveMysteryWordState(dateStr: String, wordId: Long, cluesRevealed: Int, isSolved: Boolean) {
        val editor = prefs.edit()
            .putString(KEY_MYSTERY_DATE, dateStr)
            .putLong(KEY_MYSTERY_WORD_ID, wordId)
            .putInt(KEY_MYSTERY_CLUES_REVEALED, cluesRevealed)
            .putBoolean(KEY_MYSTERY_SOLVED, isSolved)
        if (isSolved) {
            val count = prefs.getInt(KEY_MYSTERY_SOLVED_COUNT, 0) + 1
            editor.putInt(KEY_MYSTERY_SOLVED_COUNT, count)
        }
        editor.apply()
    }

    var mysteryWordsSolvedCount: Int
        get() = prefs.getInt(KEY_MYSTERY_SOLVED_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_MYSTERY_SOLVED_COUNT, value).apply()

    // Phase 2: Achievements
    fun getUnlockedAchievements(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()) ?: emptySet()
    }

    fun unlockAchievement(id: String): Boolean {
        val current = getUnlockedAchievements().toMutableSet()
        if (current.add(id)) {
            prefs.edit().putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, current).apply()
            return true
        }
        return false
    }

    var perfectGamesCount: Int
        get() = prefs.getInt(KEY_PERFECT_GAMES_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PERFECT_GAMES_COUNT, value).apply()

    fun recordPerfectGame() {
        perfectGamesCount += 1
    }

    // Phase 2: Weekly League
    fun getLeagueData(currentWeekKey: String): Pair<String, Int> {
        val savedWeek = prefs.getString(KEY_LEAGUE_WEEK_KEY, "") ?: ""
        return if (savedWeek == currentWeekKey) {
            Pair(savedWeek, prefs.getInt(KEY_LEAGUE_XP, 0))
        } else {
            // New week, reset weekly XP
            prefs.edit().putString(KEY_LEAGUE_WEEK_KEY, currentWeekKey).putInt(KEY_LEAGUE_XP, 0).apply()
            Pair(currentWeekKey, 0)
        }
    }

    fun addLeagueXp(currentWeekKey: String, xp: Int) {
        val (_, currentXp) = getLeagueData(currentWeekKey)
        prefs.edit()
            .putString(KEY_LEAGUE_WEEK_KEY, currentWeekKey)
            .putInt(KEY_LEAGUE_XP, currentXp + xp)
            .apply()
    }

    // Phase 2: Events
    fun getCompletedEvents(): Set<String> {
        return prefs.getStringSet(KEY_COMPLETED_EVENTS, emptySet()) ?: emptySet()
    }

    fun markEventCompleted(eventId: String) {
        val set = getCompletedEvents().toMutableSet()
        set.add(eventId)
        prefs.edit().putStringSet(KEY_COMPLETED_EVENTS, set).apply()
    }

    // Phase 2: Pronunciation
    var pronunciationCompletedCount: Int
        get() = prefs.getInt(KEY_PRONUNCIATION_COMPLETED_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PRONUNCIATION_COMPLETED_COUNT, value).apply()

    // Phase 3: Progression & Sound / Haptics
    var arcadeTotalXp: Int
        get() = prefs.getInt(KEY_ARCADE_TOTAL_XP, 0)
        set(value) = prefs.edit().putInt(KEY_ARCADE_TOTAL_XP, value).apply()

    fun addArcadeXp(amount: Int): Int {
        val newXp = (arcadeTotalXp + amount).coerceAtLeast(0)
        arcadeTotalXp = newXp
        return newXp
    }

    var selectedPlayerTitle: String
        get() = prefs.getString(KEY_SELECTED_PLAYER_TITLE, "WORD BEGINNER") ?: "WORD BEGINNER"
        set(value) = prefs.edit().putString(KEY_SELECTED_PLAYER_TITLE, value).apply()

    var isSoundEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_EFFECTS_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    fun resetWallpaperConfig() {
        wallpaperConfig = WallpaperConfig(
            style = WallpaperStyle.OBSIDIAN,
            positionPreset = com.sarthak.lexilock.data.model.VocabPosition.UPPER_CENTER,
            posX = 0.5f,
            posY = 0.28f,
            wordSizeScale = 1.0f,
            ipaSizeScale = 1.0f,
            definitionSizeScale = 1.0f,
            exampleSizeScale = 1.0f,
            letterSpacing = 0.0f,
            lineSpacingScale = 1.0f,
            alignment = com.sarthak.lexilock.data.model.TextAlignment.CENTER,
            displayFont = com.sarthak.lexilock.data.model.DisplayFont.EDITORIAL_SERIF,
            bodyFont = com.sarthak.lexilock.data.model.BodyFont.EDITORIAL_SANS,
            showWord = true,
            showIpa = true,
            showPartOfSpeech = true,
            showDefinition = true,
            showExample = true,
            showSpeaker = true,
            showStreak = true,
            textColorPreset = com.sarthak.lexilock.data.model.TextColorPreset.WARM_WHITE,
            customTextColorHex = "#FAF7EE",
            accentColorHex = "#7E96B8"
        )
    }

    fun resetProgress() {
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, 0)
            .putInt(KEY_LONGEST_STREAK, 0)
            .putString(KEY_LAST_LEARNED_DATE, "")
            .putInt(KEY_TOTAL_WORDS_LEARNED, 0)
            .putInt(KEY_USER_XP, 0)
            .putStringSet(KEY_UNLOCKED_BADGES_JSON, emptySet())
            .putInt(KEY_COMPLETED_CHALLENGES_COUNT, 0)
            .putInt(KEY_TTS_SESSIONS_COUNT, 0)
            .putLong(KEY_TODAY_WORD_ID, -1L)
            .putString(KEY_TODAY_WORD_IDS, "")
            .putString(KEY_TODAY_DATE, "")
            .putString(KEY_CACHED_LEVEL, "")
            .putString(KEY_WALLPAPER_WORD_MODE, "daily")
            .putString(KEY_MANUAL_WALLPAPER_WORD, "")
            .apply()
    }
}
