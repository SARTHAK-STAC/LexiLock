package com.sarthak.lexilock.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.notifications.DailyWordNotificationScheduler
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.components.LexiIcons
import com.sarthak.lexilock.ui.theme.*
import com.sarthak.lexilock.wallpaper.LexiLockWallpaperService
import com.sarthak.lexilock.wallpaper.WallpaperHelper

@Composable
fun SettingsScreen(
    context: Context,
    prefs: LexiPreferences,
    repository: WordRepository,
    onEnableWallpaperClick: () -> Unit,
    onOpenStudioClick: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    var englishLevel by remember { mutableStateOf(prefs.englishLevel) }
    var wordsPerDay by remember { mutableIntStateOf(prefs.wordsPerDay) }

    var isWallpaperActive by remember { mutableStateOf(WallpaperHelper.isWallpaperActive(context)) }
    var wallpaperStyle by remember { mutableStateOf(prefs.wallpaperStyle) }
    var showDailyWordOnLock by remember { mutableStateOf(prefs.showDailyWordOnLock) }
    var showPronunciationOnLock by remember { mutableStateOf(prefs.showPronunciationOnLock) }
    var showDefinitionOnLock by remember { mutableStateOf(prefs.showDefinitionOnLock) }
    var showExampleOnLock by remember { mutableStateOf(prefs.showExampleOnLock) }
    var showStreakOnLock by remember { mutableStateOf(prefs.showStreakOnLock) }

    var dailyNotifEnabled by remember { mutableStateOf(prefs.isDailyNotificationEnabled) }
    var notifHour by remember { mutableIntStateOf(prefs.notificationHour) }
    var notifMinute by remember { mutableIntStateOf(prefs.notificationMinute) }

    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var wallpaperWordMode by remember { mutableStateOf(prefs.wallpaperWordMode) }
    var manualWordText by remember { mutableStateOf(prefs.manualWallpaperWord) }
    var soundEffectsEnabled by remember { mutableStateOf(prefs.isSoundEffectsEnabled) }
    var hapticsEnabled by remember { mutableStateOf(prefs.isHapticsEnabled) }

    // Dialog states
    var showLevelDialog by remember { mutableStateOf(false) }
    var showWordsPerDayDialog by remember { mutableStateOf(false) }
    var showWordModeDialog by remember { mutableStateOf(false) }
    var showWallpaperStyleDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isWallpaperActive = WallpaperHelper.isWallpaperActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    LexiIcons.Back(modifier = Modifier.size(24.dp), tint = LexiDarkTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "Settings",
                color = LexiDarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: LEARNING
        SectionLabel("LEARNING")
        SettingsRow(
            title = "Learning level",
            value = englishLevel.replaceFirstChar { it.uppercase() },
            onClick = { showLevelDialog = true }
        )
        SettingsDivider()
        SettingsRow(
            title = "Words per day",
            value = if (wordsPerDay == 1) "1 word" else "$wordsPerDay words",
            onClick = { showWordsPerDayDialog = true }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: LOCK SCREEN
        SectionLabel("LOCK SCREEN")
        SettingsRow(
            title = "Live wallpaper status",
            value = if (isWallpaperActive) "On" else "Off",
            onClick = {
                // Refresh active status check
                isWallpaperActive = WallpaperHelper.isWallpaperActive(context)
            },
            valueColor = if (isWallpaperActive) LexiDarkAccent else LexiDarkTextSecondary
        )
        SettingsDivider()
        SettingsRow(
            title = "Wallpaper Studio",
            value = "Customize →",
            onClick = onOpenStudioClick,
            valueColor = LexiDarkAccent
        )
        SettingsDivider()
        SettingsRow(
            title = "Set wallpaper",
            value = "Open preview →",
            onClick = onEnableWallpaperClick
        )
        SettingsDivider()
        SettingsRow(
            title = "Wallpaper style",
            value = wallpaperStyle.replaceFirstChar { it.uppercase() },
            onClick = { showWallpaperStyleDialog = true }
        )
        SettingsDivider()
        SettingsRow(
            title = "Wallpaper word mode",
            value = if (wallpaperWordMode == "manual") "Manual Pin (${manualWordText.ifBlank { "None" }})" else "Daily Rotation ($wordsPerDay/day)",
            onClick = { showWordModeDialog = true },
            valueColor = if (wallpaperWordMode == "manual") LexiDarkAccent else LexiDarkTextPrimary
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Show daily word",
            checked = showDailyWordOnLock,
            onCheckedChange = {
                showDailyWordOnLock = it
                prefs.showDailyWordOnLock = it
                LexiLockWallpaperService.requestRedraw()
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Pronunciation",
            checked = showPronunciationOnLock,
            onCheckedChange = {
                showPronunciationOnLock = it
                prefs.showPronunciationOnLock = it
                LexiLockWallpaperService.requestRedraw()
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Show definition",
            checked = showDefinitionOnLock,
            onCheckedChange = {
                showDefinitionOnLock = it
                prefs.showDefinitionOnLock = it
                LexiLockWallpaperService.requestRedraw()
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Show example",
            checked = showExampleOnLock,
            onCheckedChange = {
                showExampleOnLock = it
                prefs.showExampleOnLock = it
                LexiLockWallpaperService.requestRedraw()
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Show streak",
            checked = showStreakOnLock,
            onCheckedChange = {
                showStreakOnLock = it
                prefs.showStreakOnLock = it
                LexiLockWallpaperService.requestRedraw()
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: NOTIFICATIONS
        SectionLabel("NOTIFICATIONS")
        SettingsSwitchRow(
            title = "Daily reminder",
            checked = dailyNotifEnabled,
            onCheckedChange = { checked ->
                dailyNotifEnabled = checked
                prefs.isDailyNotificationEnabled = checked
                if (checked) {
                    DailyWordNotificationScheduler.scheduleDailyNotification(context)
                    Toast.makeText(context, "Daily reminder scheduled", Toast.LENGTH_SHORT).show()
                } else {
                    DailyWordNotificationScheduler.cancelNotification(context)
                    Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        )
        if (dailyNotifEnabled) {
            SettingsDivider()
            SettingsRow(
                title = "Reminder time",
                value = formatTime(notifHour, notifMinute),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, min ->
                            DailyWordNotificationScheduler.cancelNotification(context)
                            notifHour = hour
                            notifMinute = min
                            prefs.notificationHour = hour
                            prefs.notificationMinute = min
                            DailyWordNotificationScheduler.scheduleDailyNotification(context)
                            Toast.makeText(context, "Reminder set for ${formatTime(hour, min)}", Toast.LENGTH_SHORT).show()
                        },
                        notifHour,
                        notifMinute,
                        false
                    ).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: APPEARANCE
        SectionLabel("APPEARANCE")
        SettingsRow(
            title = "Theme",
            value = when (themeMode) {
                "light" -> "Light"
                "dark" -> "Dark"
                else -> "System default"
            },
            onClick = { showThemeDialog = true }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: INTERACTION
        SectionLabel("INTERACTION")
        SettingsSwitchRow(
            title = "Sound effects",
            checked = soundEffectsEnabled,
            onCheckedChange = {
                soundEffectsEnabled = it
                prefs.isSoundEffectsEnabled = it
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Haptic feedback",
            checked = hapticsEnabled,
            onCheckedChange = {
                hapticsEnabled = it
                prefs.isHapticsEnabled = it
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: DATA
        SectionLabel("DATA")
        SettingsRow(
            title = "Reset progress",
            value = "Reset",
            onClick = { showResetConfirmDialog = true },
            valueColor = LexiHeartUnderstated
        )

        Spacer(modifier = Modifier.height(36.dp))
    }

    // 1. LEARNING LEVEL DIALOG
    if (showLevelDialog) {
        val levels = listOf("Beginner", "Intermediate", "Advanced", "Expert")
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("Learning level", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    levels.forEach { lvl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    englishLevel = lvl.lowercase()
                                    prefs.englishLevel = lvl.lowercase()
                                    LexiLockWallpaperService.requestRedraw()
                                    showLevelDialog = false
                                    Toast.makeText(context, "Level set to $lvl", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lvl, color = LexiDarkTextPrimary, fontSize = 15.sp)
                            if (englishLevel.equals(lvl, ignoreCase = true)) {
                                Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                            }
                        }
                        if (lvl != levels.last()) SettingsDivider()
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 2. WORDS PER DAY DIALOG
    if (showWordsPerDayDialog) {
        val options = listOf(1, 2, 3, 5)
        AlertDialog(
            onDismissRequest = { showWordsPerDayDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("Words per day", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    options.forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    wordsPerDay = count
                                    prefs.wordsPerDay = count
                                    LexiLockWallpaperService.requestRedraw(context)
                                    showWordsPerDayDialog = false
                                    Toast.makeText(context, "Set to $count words per day (${24 / count}h per slot)", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (count == 1) "1 word (24 hours)" else "$count words (${24 / count}h per slot)",
                                color = LexiDarkTextPrimary,
                                fontSize = 15.sp
                            )
                            if (wordsPerDay == count) {
                                Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                            }
                        }
                        if (count != options.last()) SettingsDivider()
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 2b. WALLPAPER WORD MODE DIALOG
    if (showWordModeDialog) {
        AlertDialog(
            onDismissRequest = { showWordModeDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("Wallpaper Word Mode", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    // DAILY ROTATION OPTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                wallpaperWordMode = "daily"
                                prefs.wallpaperWordMode = "daily"
                                LexiLockWallpaperService.requestRedraw(context)
                                showWordModeDialog = false
                                Toast.makeText(context, "Switched to Daily Words rotation schedule", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Words Schedule", color = LexiDarkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Automatically rotates $wordsPerDay times a day (${24 / wordsPerDay}h per word)", color = LexiDarkTextSecondary, fontSize = 12.sp)
                        }
                        if (wallpaperWordMode == "daily") {
                            Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                        }
                    }

                    SettingsDivider()

                    // MANUAL PIN OPTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                wallpaperWordMode = "manual"
                                prefs.wallpaperWordMode = "manual"
                                if (prefs.manualWallpaperWord.isBlank()) {
                                    prefs.manualWallpaperWord = repository.getCurrentScheduledWord().word
                                    manualWordText = prefs.manualWallpaperWord
                                }
                                LexiLockWallpaperService.requestRedraw(context)
                                showWordModeDialog = false
                                Toast.makeText(context, "Pinned to “${prefs.manualWallpaperWord}”", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manual Word Pin", color = LexiDarkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (manualWordText.isNotBlank()) "Stays permanently on “$manualWordText” until you change it"
                                else "Locks wallpaper to one selected vocabulary word",
                                color = LexiDarkTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (wallpaperWordMode == "manual") {
                            Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 3. WALLPAPER STYLE DIALOG
    if (showWallpaperStyleDialog) {
        val styles = listOf("Obsidian", "Minimal", "Aurora", "Paper", "Monochrome", "Photo", "Editorial", "Museum", "Ink", "Noir")
        AlertDialog(
            onDismissRequest = { showWallpaperStyleDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("Wallpaper style", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    styles.forEach { st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    wallpaperStyle = st.lowercase()
                                    prefs.wallpaperStyle = st.lowercase()
                                    LexiLockWallpaperService.requestRedraw()
                                    showWallpaperStyleDialog = false
                                    Toast.makeText(context, "Wallpaper style set to $st", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(st, color = LexiDarkTextPrimary, fontSize = 15.sp)
                            if (wallpaperStyle.equals(st, ignoreCase = true)) {
                                Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                            }
                        }
                        if (st != styles.last()) SettingsDivider()
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 4. THEME SELECTION DIALOG
    if (showThemeDialog) {
        val themes = listOf(
            "System" to "system",
            "Dark" to "dark",
            "Light" to "light"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("App theme", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Column {
                    themes.forEach { (label, key) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = key
                                    prefs.themeMode = key
                                    onThemeChange(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = LexiDarkTextPrimary, fontSize = 15.sp)
                            if (themeMode == key) {
                                Text("✓", color = LexiDarkAccent, fontSize = 16.sp)
                            }
                        }
                        if (key != themes.last().second) SettingsDivider()
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 5. RESET CONFIRMATION DIALOG
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = LexiDarkSurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            title = { Text("Reset progress?", color = LexiDarkTextPrimary, fontSize = 17.sp) },
            text = {
                Text(
                    text = "This will reset your current streak, mastered words, and learning statistics. Your vocabulary database will remain intact.",
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.resetAllLearningProgress()
                        LexiLockWallpaperService.requestRedraw()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Progress reset successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = LexiHeartUnderstated, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = LexiDarkTextSecondary)
                }
            }
        )
    }
}

private fun formatTime(hour: Int, min: Int): String {
    val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    val amPm = if (hour >= 12) "PM" else "AM"
    return String.format("%02d:%02d %s", h, min, amPm)
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        color = LexiDarkTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    LexiDivider(color = LexiDarkDivider)
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = LexiDarkTextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = LexiDarkTextPrimary,
            fontSize = 15.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = LexiDarkTextPrimary,
            fontSize = 15.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LexiDarkAccent,
                uncheckedThumbColor = LexiDarkTextSecondary,
                uncheckedTrackColor = LexiDarkSurfaceSubtle,
                uncheckedBorderColor = LexiDarkDivider
            )
        )
    }
}
