package com.sarthak.lexilock.ui.home

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.*
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.components.LexiIcons
import com.sarthak.lexilock.ui.components.LexiProgressBar
import com.sarthak.lexilock.ui.theme.*
import com.sarthak.lexilock.utils.FeedbackType
import com.sarthak.lexilock.utils.LexiSoundHapticsManager
import com.sarthak.lexilock.utils.ShareCardGenerator
import com.sarthak.lexilock.wallpaper.LexiLockWallpaperService
import com.sarthak.lexilock.wallpaper.WallpaperHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    context: Context,
    repository: WordRepository,
    prefs: LexiPreferences,
    onWordClick: (WordItem) -> Unit,
    onEnableWallpaperClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenStudioClick: (() -> Unit)? = null,
    onOpenArcadeClick: (() -> Unit)? = null,
    onNavigateToProgress: (() -> Unit)? = null,
    onOpenDailyChallenge: ((MiniGameType) -> Unit)? = null
) {
    val dailyChallengeManager = remember { DailyChallengeGameManager(prefs) }
    val eventRepository = remember { EventRepository(prefs, repository) }
    val communityWord = remember { CommunityWordManager.getTodayCommunityWord(repository) }

    var todayWords by remember { mutableStateOf(repository.getTodayWords()) }
    var scheduledSlots by remember { mutableStateOf(repository.getDailyWordSchedule()) }
    var isManualMode by remember { mutableStateOf(prefs.wallpaperWordMode == "manual") }
    var manualWordText by remember { mutableStateOf(prefs.manualWallpaperWord) }
    var currentIndex by remember { mutableIntStateOf(0) }

    var isMysteryDialogOpen by remember { mutableStateOf(false) }

    // Synchronize initial currentIndex with current scheduled slot if available
    LaunchedEffect(scheduledSlots) {
        val activeIndex = scheduledSlots.indexOfFirst { it.isCurrent }
        if (activeIndex >= 0) {
            currentIndex = activeIndex
        }
    }

    val safeIndex = if (todayWords.isNotEmpty()) currentIndex.coerceIn(0, todayWords.size - 1) else 0
    val currentWord = if (todayWords.isNotEmpty()) todayWords[safeIndex] else repository.getTodayWord()
    val currentSlot = scheduledSlots.getOrNull(safeIndex)

    // Level progression
    val userXp = prefs.userXp
    val levelInfo = remember(userXp) { LevelManager.getLevelInfo(userXp) }
    val currentEvent = remember { eventRepository.getCurrentEvent() }
    val dailyChallengeInfo = remember { dailyChallengeManager.getTodayChallengeInfo() }

    // TextToSpeech initialization
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    val dateFormatted = remember {
        val cal = Calendar.getInstance()
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(cal.time)
    }

    val weeklyActivity = remember(currentWord.isMastered) { repository.getWeeklyActivity() }
    val dayKeys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val activeDaysCount = weeklyActivity.values.count { it > 0 }

    var showPronunciation by remember { mutableStateOf(prefs.showPronunciationOnLock) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                showPronunciation = prefs.showPronunciationOnLock
                todayWords = repository.getTodayWords()
                scheduledSlots = repository.getDailyWordSchedule()
                isManualMode = (prefs.wallpaperWordMode == "manual")
                manualWordText = prefs.manualWallpaperWord
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    if (isMysteryDialogOpen) {
        MysteryWordDialog(
            context = context,
            prefs = prefs,
            repository = repository,
            onDismissRequest = { isMysteryDialogOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // TOP EDITORIAL HEADER WITH LEVEL BADGE & STREAK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting, Sarthak",
                    color = LexiDarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Level ${levelInfo.levelNumber} • ${levelInfo.currentLevelXp} / ${levelInfo.xpForNextLevel} XP",
                    color = LexiDarkAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Subtle Streak Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(text = "🔥", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${prefs.currentStreak}",
                        color = LexiDarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Settings Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onOpenSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    LexiIcons.Gear(modifier = Modifier.size(18.dp), tint = LexiDarkTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MANUAL MODE BANNER IF ACTIVE
        if (isManualMode && manualWordText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "📌", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wallpaper pinned to “$manualWordText”",
                        color = LexiDarkAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Resume Daily",
                    color = LexiDarkTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            prefs.wallpaperWordMode = "daily"
                            isManualMode = false
                            LexiLockWallpaperService.requestRedraw(context)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // MULTI-WORD ROTATION SCHEDULE NAVIGATION
        if (todayWords.size > 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SLOT ${safeIndex + 1} OF ${todayWords.size}",
                            color = LexiDarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )

                        if (currentSlot?.isCurrent == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE",
                                color = LexiDarkAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LexiDarkAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "← Prev",
                            color = if (safeIndex > 0) LexiDarkTextPrimary else LexiDarkTextSecondary.copy(alpha = 0.35f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = safeIndex > 0) {
                                    LexiSoundHapticsManager.performHaptic(context, FeedbackType.LIGHT_TICK)
                                    currentIndex = safeIndex - 1
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next →",
                            color = if (safeIndex < todayWords.size - 1) LexiDarkTextPrimary else LexiDarkTextSecondary.copy(alpha = 0.35f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = safeIndex < todayWords.size - 1) {
                                    LexiSoundHapticsManager.performHaptic(context, FeedbackType.LIGHT_TICK)
                                    currentIndex = safeIndex + 1
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                if (currentSlot != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Slot: ${currentSlot.timeRangeText}",
                            color = LexiDarkTextSecondary,
                            fontSize = 12.sp
                        )
                        if (currentSlot.isCurrent) {
                            Text(
                                text = currentSlot.countdownText,
                                color = LexiDarkAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // MAIN EDITORIAL VOCABULARY SECTION (TODAY'S WORD HERO)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onWordClick(currentWord) }
        ) {
            Text(
                text = currentWord.word.replaceFirstChar { it.uppercase() },
                color = LexiDarkTextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Serif,
                lineHeight = 48.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Part of Speech & Pronunciation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentWord.partOfSpeech.lowercase(),
                    color = LexiDarkTextSecondary,
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic
                )
                if (showPronunciation && currentWord.pronunciation.isNotBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentWord.pronunciation,
                        color = LexiDarkTextSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Definition
            Text(
                text = currentWord.definition,
                color = LexiDarkTextPrimary,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Example Sentence
            Text(
                text = "“${currentWord.exampleSentence}”",
                color = LexiDarkTextSecondary,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Inline Actions: Pronounce, Save, Share Card, Mastery
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Audio Pronounce Button
                if (showPronunciation) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LexiDarkSurfaceSubtle)
                            .clickable {
                                ttsInstance?.speak(
                                    currentWord.word,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "tts_home"
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LexiIcons.Sound(modifier = Modifier.size(16.dp), tint = LexiDarkTextPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }

                // 2. Save Toggle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LexiDarkSurfaceSubtle)
                        .clickable {
                            LexiSoundHapticsManager.performHaptic(context, FeedbackType.LIGHT_TICK)
                            val newSave = !currentWord.isSaved
                            repository.toggleSave(currentWord.id, newSave)
                            todayWords = todayWords.toMutableList().also {
                                it[safeIndex] = currentWord.copy(isSaved = newSave)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LexiIcons.Heart(
                        modifier = Modifier.size(16.dp),
                        tint = if (currentWord.isSaved) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        filled = currentWord.isSaved
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 3. Editorial Share Card Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LexiDarkSurfaceSubtle)
                        .clickable {
                            LexiSoundHapticsManager.performHaptic(context, FeedbackType.LIGHT_TICK)
                            ShareCardGenerator.shareWordCard(context, currentWord)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↗", color = LexiDarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 4. Mastery Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val newMastered = !currentWord.isMastered
                            if (newMastered) {
                                LexiSoundHapticsManager.performHaptic(context, FeedbackType.CORRECT)
                                LexiSoundHapticsManager.performSound(context, FeedbackType.CORRECT)
                            } else {
                                LexiSoundHapticsManager.performHaptic(context, FeedbackType.LIGHT_TICK)
                            }
                            repository.markAsMastered(currentWord.id, newMastered)
                            todayWords = todayWords.toMutableList().also {
                                it[safeIndex] = currentWord.copy(isMastered = newMastered)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentWord.isMastered) "Mastered ✓" else "Mark as mastered →",
                        color = if (currentWord.isMastered) LexiDarkTextSecondary else LexiDarkAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(22.dp))

        // RETENTION CARD 1: TODAY'S CHALLENGE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .clickable {
                    if (onOpenDailyChallenge != null) {
                        onOpenDailyChallenge(dailyChallengeInfo.gameType)
                    } else if (onOpenArcadeClick != null) {
                        onOpenArcadeClick()
                    }
                }
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎯", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S CHALLENGE",
                        color = LexiDarkAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dailyChallengeInfo.gameType.title,
                    color = LexiDarkTextPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (dailyChallengeInfo.isCompleted) "Completed (+${dailyChallengeInfo.bonusXp} XP)" else "+${dailyChallengeInfo.bonusXp} XP bonus for today's test",
                    color = if (dailyChallengeInfo.isCompleted) LexiCorrectGreen else LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = if (dailyChallengeInfo.isCompleted) "Replay →" else "Play →",
                color = LexiDarkAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RETENTION CARD 2: TODAY'S MYSTERY WORD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .clickable { isMysteryDialogOpen = true }
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔍", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MYSTERY WORD",
                        color = LexiDarkAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deduce from clues",
                    color = LexiDarkTextPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Progressive hints with decaying rewards",
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "Solve →",
                color = LexiDarkTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RETENTION CARD 3: COMMUNITY FEATURED WORD & ETYMOLOGY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMUNITY WORD OF THE DAY",
                    color = LexiDarkTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Curator Pick",
                    color = LexiDarkAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = communityWord.wordItem.word.replaceFirstChar { it.uppercase() },
                color = LexiDarkTextPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = communityWord.etymologyFact,
                color = LexiDarkTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RETENTION CARD 4: LIMITED TIME EVENT BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentEvent.title.uppercase()} • ${currentEvent.daysRemaining}D LEFT",
                    color = LexiDarkAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentEvent.description,
                    color = LexiDarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentEvent.isCompleted) "Completed (+${currentEvent.bonusXp} XP)" else "Progress: ${currentEvent.currentProgress} / ${currentEvent.targetWordCount} Mastered",
                    color = if (currentEvent.isCompleted) LexiCorrectGreen else LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(18.dp))

        // WALLPAPER STUDIO QUICK LAUNCH
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (onOpenStudioClick != null) onOpenStudioClick() else onEnableWallpaperClick()
                }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LexiIcons.Lock(modifier = Modifier.size(15.dp), tint = LexiDarkTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wallpaper Studio & Drops",
                    color = LexiDarkTextSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "Studio →",
                color = LexiDarkAccent,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
