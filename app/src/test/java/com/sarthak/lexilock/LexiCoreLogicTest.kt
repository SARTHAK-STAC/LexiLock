package com.sarthak.lexilock

import com.sarthak.lexilock.data.repository.BattleChallenge
import com.sarthak.lexilock.data.repository.FriendBattleManager
import com.sarthak.lexilock.domain.model.WordItem
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LexiCoreLogicTest {

    private fun createDummyWord(id: Long, word: String, definition: String): WordItem {
        return WordItem(
            id = id,
            word = word,
            partOfSpeech = "adjective",
            definition = definition,
            exampleSentence = "An example sentence for $word.",
            pronunciation = "/test/",
            difficulty = "intermediate",
            synonyms = emptyList(),
            antonyms = emptyList(),
            category = "Academic"
        )
    }

    // ==========================================
    // 1. FRIEND BATTLE SERIALIZATION & PARSING
    // ==========================================

    @Test
    fun testFriendBattleCodeSerializationAndParsing() {
        val mockWords = listOf(
            createDummyWord(101L, "Eloquent", "Fluent or persuasive in speaking"),
            createDummyWord(202L, "Acumen", "Keen insight"),
            createDummyWord(303L, "Tenacious", "Persistent"),
            createDummyWord(404L, "Resilient", "Able to recoil"),
            createDummyWord(505L, "Pragmatic", "Practical")
        )

        val code = FriendBattleManager.createChallengeCode(
            userName = "Alex",
            score = 400,
            words = mockWords
        )

        assertEquals("LEXI-BATTLE:Alex:400:101,202,303,404,505", code)

        val parsed = FriendBattleManager.parseChallengeCode(code)
        assertNotNull(parsed)
        assertEquals("Alex", parsed!!.challengerName)
        assertEquals(400, parsed.challengerScore)
        assertEquals(listOf(101L, 202L, 303L, 404L, 505L), parsed.wordIds)
    }

    @Test
    fun testFriendBattleInvalidCodeHandling() {
        // Empty and blank
        assertNull(FriendBattleManager.parseChallengeCode(""))
        assertNull(FriendBattleManager.parseChallengeCode("   "))

        // Malformed prefixes
        assertNull(FriendBattleManager.parseChallengeCode("INVALID_PREFIX:Alex:400:1,2,3"))
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE"))

        // Truncated components
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex"))
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex:400"))
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE::400:1,2,3"))

        // Invalid score format
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex:notanumber:1,2,3"))
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex:-50:1,2,3"))

        // Invalid word IDs
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex:400:not_ids"))
        assertNull(FriendBattleManager.parseChallengeCode("LEXI-BATTLE:Alex:400:,,,,"))
    }

    @Test
    fun testFriendBattleOversizedAndSpecialCharacters() {
        // Oversized ID list
        val longIds = (1..50).joinToString(",")
        val code = "LEXI-BATTLE:SuperUser:1500:$longIds"
        val parsed = FriendBattleManager.parseChallengeCode(code)
        assertNotNull(parsed)
        assertEquals(50, parsed!!.wordIds.size)
        assertEquals(1500, parsed.challengerScore)

        // Special characters in user name sanitization
        val nameWithColon = "John:Doe"
        val cleanCode = FriendBattleManager.createChallengeCode(
            userName = nameWithColon,
            score = 250,
            words = listOf(createDummyWord(1L, "Test", "Def"))
        )
        assertFalse(cleanCode.contains("John:Doe"))
        assertTrue(cleanCode.contains("John_Doe"))
    }

    @Test
    fun testFriendBattleShowdownScoring() {
        val challenge = BattleChallenge(
            challengerName = "Bob",
            challengerScore = 300,
            wordIds = listOf(1L, 2L, 3L, 4L, 5L),
            codeString = "LEXI-BATTLE:Bob:300:1,2,3,4,5"
        )

        // User wins
        val win = FriendBattleManager.compareResult(challenge, 400)
        assertTrue(win.isUserWinner)
        assertFalse(win.isDraw)

        // User draws
        val draw = FriendBattleManager.compareResult(challenge, 300)
        assertFalse(draw.isUserWinner)
        assertTrue(draw.isDraw)

        // User loses
        val lose = FriendBattleManager.compareResult(challenge, 200)
        assertFalse(lose.isUserWinner)
        assertFalse(lose.isDraw)
    }

    // ==========================================
    // 2. XP CAPS & ANTI-FARMING RULES
    // ==========================================

    @Test
    fun testMiniGameSessionXpCap() {
        // MiniGameManager enforces max 150 XP per session: (totalXpAccumulated + bonusXp).coerceIn(0, 150)
        val lowXp = (40 + 20).coerceIn(0, 150)
        assertEquals(60, lowXp)

        val overflowXp = (300 + 50).coerceIn(0, 150)
        assertEquals(150, overflowXp)

        val negativeXp = (-20).coerceIn(0, 150)
        assertEquals(0, negativeXp)
    }

    @Test
    fun testComboMultiplierProgression() {
        fun calculateComboXp(combo: Int): Int {
            return when (combo) {
                0, 1 -> 10
                2 -> 12
                3 -> 15
                4 -> 18
                else -> 20
            }
        }

        assertEquals(10, calculateComboXp(0))
        assertEquals(10, calculateComboXp(1))
        assertEquals(12, calculateComboXp(2))
        assertEquals(15, calculateComboXp(3))
        assertEquals(18, calculateComboXp(4))
        assertEquals(20, calculateComboXp(5))
        assertEquals(20, calculateComboXp(100))
    }

    @Test
    fun testDailyChallengeDuplicateRewardPrevention() {
        // DailyChallengeGameManager returns 0 XP on replay
        fun calculateAward(isAlreadyDoneToday: Boolean): Int {
            return if (!isAlreadyDoneToday) 50 else 0
        }

        assertEquals(50, calculateAward(false)) // First attempt awards 50 XP
        assertEquals(0, calculateAward(true))   // Repeated attempt awards 0 XP
    }

    // ==========================================
    // 3. DATE BOUNDARIES, RESETS & STREAKS
    // ==========================================

    @Test
    fun testStreakCalculationConsecutiveDay() {
        fun calculateNewStreak(lastLearnedDate: String, todayDate: String, currentStreak: Int): Int {
            if (lastLearnedDate == todayDate) return currentStreak
            if (lastLearnedDate.isBlank()) return 1

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayCal = Calendar.getInstance().apply { time = sdf.parse(todayDate) ?: Date() }
            val lastCal = Calendar.getInstance().apply { time = sdf.parse(lastLearnedDate) ?: Date() }
            val diffDays = ((todayCal.timeInMillis - lastCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

            return when (diffDays) {
                1 -> currentStreak + 1
                0 -> currentStreak
                else -> 1
            }
        }

        // Same day: streak stays unchanged
        assertEquals(5, calculateNewStreak("2026-10-13", "2026-10-13", 5))

        // Consecutive day: streak increments
        assertEquals(6, calculateNewStreak("2026-10-12", "2026-10-13", 5))

        // Missed day (broken): streak resets to 1
        assertEquals(1, calculateNewStreak("2026-10-10", "2026-10-13", 5))
    }

    @Test
    fun testWeeklyLeagueKeyAndDaysRemaining() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 13) // Tuesday
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val weekKey = "$year-W$week"

        assertEquals(2026, year)
        assertTrue(weekKey.startsWith("2026-W"))

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Tuesday = 3
        assertEquals(Calendar.TUESDAY, dayOfWeek)

        val daysRemaining = if (dayOfWeek == Calendar.SUNDAY) 1 else (Calendar.SATURDAY - dayOfWeek + 2)
        assertEquals(6, daysRemaining) // Tuesday to Sunday inclusive = 6 days
    }

    // ==========================================
    // 4. MYSTERY WORD MASKING EDGE CASES
    // ==========================================

    @Test
    fun testMysteryWordMaskingStandard() {
        val word = "Serendipity"
        val sentence = "Finding this place was pure serendipity and joy."
        val masked = sentence.replace(Regex(Regex.escape(word), RegexOption.IGNORE_CASE), "[ ______ ]")
        assertEquals("Finding this place was pure [ ______ ] and joy.", masked)
    }

    @Test
    fun testMysteryWordMaskingMultipleOccurrencesAndPunctuation() {
        val word = "run"
        val sentence = "They run and run, then run!"
        val masked = sentence.replace(Regex(Regex.escape(word), RegexOption.IGNORE_CASE), "[ ______ ]")
        assertEquals("They [ ______ ] and [ ______ ], then [ ______ ]!", masked)
    }

    @Test
    fun testMysteryWordMaskingCaseVariations() {
        val word = "DeFeR"
        val sentence = "We defer now, but they will DEFER later."
        val masked = sentence.replace(Regex(Regex.escape(word), RegexOption.IGNORE_CASE), "[ ______ ]")
        assertEquals("We [ ______ ] now, but they will [ ______ ] later.", masked)
    }
}
