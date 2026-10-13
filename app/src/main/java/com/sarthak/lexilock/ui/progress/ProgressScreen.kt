package com.sarthak.lexilock.ui.progress

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.*
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.components.LexiProgressBar
import com.sarthak.lexilock.ui.theme.*
import com.sarthak.lexilock.utils.ShareCardGenerator

@Composable
fun ProgressScreen(
    context: Context,
    repository: WordRepository,
    prefs: LexiPreferences
) {
    val currentStreak = prefs.currentStreak
    val totalWordsLearned = prefs.totalWordsLearned
    val masteredWords = remember { repository.getMasteredWordsCount() }
    val userXp = prefs.userXp
    val levelInfo = remember(userXp) { LevelManager.getLevelInfo(userXp) }
    val learnerProfile = remember { LearnerProfileManager.determineProfile(prefs, repository) }
    val achievements = remember { AchievementManager.checkAndUnlockAll(prefs, repository) }
    val leagueState = remember { WeeklyLeagueManager.getLeagueState(prefs) }

    val weeklyActivity = remember { repository.getWeeklyActivity() }
    val dayKeys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var activeTitle by remember { mutableStateOf(PlayerTitleManager.getActiveTitle(prefs)) }
    var showTitleDialog by remember { mutableStateOf(false) }
    val titleList = remember(showTitleDialog) { PlayerTitleManager.getTitles(prefs, repository) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your progress",
                color = LexiDarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Streak milestone share button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .clickable {
                        ShareCardGenerator.shareStreakMilestone(context, currentStreak)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔥", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Share Streak ↗",
                    color = LexiDarkAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // STREAK HERO (EDITORIAL, CALM)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$currentStreak day streak",
                color = LexiDarkTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // PART 12 — LEARNER LEVEL & XP PROGRESSION
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEVEL ${levelInfo.levelNumber}",
                        color = LexiDarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = levelInfo.title,
                        color = LexiDarkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${levelInfo.currentLevelXp} / ${levelInfo.xpForNextLevel} XP",
                    color = LexiDarkAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LexiProgressBar(progress = levelInfo.progressFraction, color = LexiDarkAccent)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Total Career: ${levelInfo.totalXp} XP",
                color = LexiDarkTextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // VOCABULARY LEARNER PROFILE: "WHAT KIND OF LEARNER ARE YOU?"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(18.dp)
        ) {
            Text(
                text = "YOUR VOCABULARY PROFILE",
                color = LexiDarkAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = learnerProfile.title,
                color = LexiDarkTextPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "“${learnerProfile.headline}”",
                color = LexiDarkTextSecondary,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = learnerProfile.description,
                color = LexiDarkTextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            LexiDivider(color = LexiDarkDivider)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Primary Strength", color = LexiDarkTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = learnerProfile.strength, color = LexiCorrectGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Recommended Growth", color = LexiDarkTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = learnerProfile.recommendedAction, color = LexiDarkTextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            LexiDivider(color = LexiDarkDivider)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showTitleDialog = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Active Title", color = LexiDarkTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeTitle,
                        color = LexiDarkAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Change ▾",
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // WEEKLY BENCHMARK LEAGUE
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY LEAGUE",
                        color = LexiDarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = leagueState.weekLabel,
                        color = LexiDarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${leagueState.daysRemaining}d remaining",
                    color = LexiDarkAccent,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            leagueState.participants.forEach { p ->
                val isUser = p.isUser
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUser) LexiDarkSurfaceSubtle else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${p.rank}",
                            color = if (p.isPromotionZone) LexiDarkAccent else LexiDarkTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = if (isUser) "You ($activeTitle)" else p.name,
                            color = if (isUser) LexiDarkTextPrimary else LexiDarkTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal
                        )
                        if (p.isPromotionZone) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "▲",
                                color = LexiCorrectGreen,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "${p.xp} XP",
                        color = if (isUser) LexiDarkAccent else LexiDarkTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Top 3 learners promote to the next tier at weekly reset.",
                color = LexiDarkTextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // SIMPLE STATISTICS (TEXT & WHITESPACE)
        StatLine(label = "Words learned", value = "$totalWordsLearned")
        Spacer(modifier = Modifier.height(12.dp))
        StatLine(label = "Mastered words", value = "$masteredWords")
        Spacer(modifier = Modifier.height(12.dp))
        StatLine(label = "Target level", value = prefs.englishLevel.replaceFirstChar { it.uppercase() })
        Spacer(modifier = Modifier.height(12.dp))
        StatLine(label = "Pronunciations practiced", value = "${prefs.pronunciationCompletedCount}")

        Spacer(modifier = Modifier.height(24.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // WEEKLY ACTIVITY (MON TUE WED THU FRI SAT SUN)
        Text(
            text = "Weekly activity",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            dayKeys.forEachIndexed { index, dayKey ->
                val count = weeklyActivity[dayKey] ?: 0
                val hasActivity = count > 0

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayLabels[index],
                        color = LexiDarkTextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (hasActivity) LexiDarkTextPrimary else LexiDarkDivider)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        LexiDivider(color = LexiDarkDivider)
        Spacer(modifier = Modifier.height(20.dp))

        // 12 REAL-EVENT ACHIEVEMENTS
        Text(
            text = "ACHIEVEMENTS (${achievements.count { it.isUnlocked }} / ${achievements.size})",
            color = LexiDarkTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        achievements.forEach { ach ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ach.iconEmoji,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ach.title,
                        color = if (ach.isUnlocked) LexiDarkTextPrimary else LexiDarkTextSecondary.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ach.description,
                        color = LexiDarkTextSecondary.copy(alpha = if (ach.isUnlocked) 0.8f else 0.4f),
                        fontSize = 12.sp
                    )
                    if (!ach.isUnlocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${ach.currentProgress} / ${ach.requiredProgress}",
                            color = LexiDarkTextSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = if (ach.isUnlocked) "✓" else "○",
                    color = if (ach.isUnlocked) LexiDarkAccent else LexiDarkTextSecondary.copy(alpha = 0.35f),
                    fontSize = 14.sp
                )
            }
            LexiDivider(color = LexiDarkDivider.copy(alpha = 0.4f))
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showTitleDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Player Titles",
                    color = LexiDarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Earned through milestones. Choose your display title for Profile, League, and Arcade.",
                        color = LexiDarkTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    titleList.forEach { titleItem ->
                        val isSelected = titleItem.title == activeTitle
                        val isUnlocked = titleItem.isUnlocked

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) LexiDarkAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable(enabled = isUnlocked) {
                                    PlayerTitleManager.setActiveTitle(prefs, titleItem.title)
                                    activeTitle = titleItem.title
                                    showTitleDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = titleItem.title,
                                        color = if (isUnlocked) LexiDarkTextPrimary else LexiDarkTextSecondary.copy(alpha = 0.4f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "• ACTIVE", color = LexiDarkAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isUnlocked) titleItem.description else "🔒 ${titleItem.unlockCondition}",
                                    color = if (isUnlocked) LexiDarkTextSecondary else LexiDarkTextSecondary.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            if (isUnlocked) {
                                Text(
                                    text = if (isSelected) "✓" else "Select",
                                    color = if (isSelected) LexiDarkAccent else LexiDarkTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        LexiDivider(color = LexiDarkDivider.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showTitleDialog = false }) {
                    Text("Close", color = LexiDarkAccent)
                }
            }
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LexiDarkTextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = LexiDarkTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
