package com.sarthak.lexilock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.notifications.DailyWordNotificationScheduler
import com.sarthak.lexilock.ui.arcade.LexiArcadeScreen
import com.sarthak.lexilock.ui.components.LexiBottomNavigationBar
import com.sarthak.lexilock.ui.components.NavigationTab
import com.sarthak.lexilock.ui.explore.ExploreScreen
import com.sarthak.lexilock.ui.home.HomeScreen
import com.sarthak.lexilock.ui.onboarding.OnboardingScreen
import com.sarthak.lexilock.ui.progress.ProgressScreen
import com.sarthak.lexilock.ui.settings.SettingsScreen
import com.sarthak.lexilock.ui.theme.LexiLockTheme
import com.sarthak.lexilock.ui.wallpaper.WallpaperStudioScreen
import com.sarthak.lexilock.ui.word.WordDetailsScreen
import com.sarthak.lexilock.wallpaper.WallpaperHelper

class MainActivity : ComponentActivity() {

    private lateinit var prefs: LexiPreferences
    private lateinit var repository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = LexiPreferences.getInstance(this)
        repository = WordRepository(this)

        // Schedule daily notification
        if (prefs.isDailyNotificationEnabled) {
            DailyWordNotificationScheduler.scheduleDailyNotification(this)
        }

        setContent {
            var currentThemeMode by remember { mutableStateOf(prefs.themeMode) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (currentThemeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            LexiLockTheme(darkTheme = isDark) {
                var isOnboardingDone by remember {
                    mutableStateOf(prefs.isOnboardingCompleted)
                }

                var currentTab by remember {
                    mutableStateOf(NavigationTab.HOME)
                }

                var isStudioOpen by remember {
                    mutableStateOf(false)
                }

                var selectedWordForDetails by remember {
                    mutableStateOf<WordItem?>(null)
                }

                // Check intent extra if opened from wallpaper or notification
                LaunchedEffect(intent) {
                    if (intent?.getStringExtra("NAVIGATE_TO") == "DETAILS") {
                        selectedWordForDetails = repository.getTodayWord()
                    }
                }

                if (!isOnboardingDone) {
                    OnboardingScreen(
                        context = this,
                        prefs = prefs,
                        onComplete = {
                            isOnboardingDone = true
                            openWallpaperChooser()
                        }
                    )
                } else {
                    // Back Navigation Management
                    BackHandler(enabled = isStudioOpen || selectedWordForDetails != null || currentTab != NavigationTab.HOME) {
                        if (selectedWordForDetails != null) {
                            selectedWordForDetails = null
                        } else if (isStudioOpen) {
                            isStudioOpen = false
                        } else if (currentTab != NavigationTab.HOME) {
                            currentTab = NavigationTab.HOME
                        }
                    }

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (selectedWordForDetails == null && !isStudioOpen) {
                                LexiBottomNavigationBar(
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (selectedWordForDetails != null) {
                                WordDetailsScreen(
                                    context = this@MainActivity,
                                    wordItem = selectedWordForDetails!!,
                                    repository = repository,
                                    onBackClick = { selectedWordForDetails = null }
                                )
                            } else if (isStudioOpen) {
                                WallpaperStudioScreen(
                                    context = this@MainActivity,
                                    prefs = prefs,
                                    repository = repository,
                                    initialWordItem = repository.getActiveWallpaperWord(),
                                    onApplyToSystemClick = { openWallpaperChooser() },
                                    onBackClick = { isStudioOpen = false }
                                )
                            } else {
                                when (currentTab) {
                                    NavigationTab.HOME -> {
                                        HomeScreen(
                                            context = this@MainActivity,
                                            repository = repository,
                                            prefs = prefs,
                                            onWordClick = { word -> selectedWordForDetails = word },
                                            onEnableWallpaperClick = { openWallpaperChooser() },
                                            onOpenSettingsClick = { currentTab = NavigationTab.SETTINGS },
                                            onOpenStudioClick = { isStudioOpen = true },
                                            onOpenArcadeClick = { currentTab = NavigationTab.ARCADE },
                                            onNavigateToProgress = { currentTab = NavigationTab.PROGRESS },
                                            onOpenDailyChallenge = { currentTab = NavigationTab.ARCADE }
                                        )
                                    }
                                    NavigationTab.EXPLORE -> {
                                        ExploreScreen(
                                            repository = repository,
                                            onWordClick = { word -> selectedWordForDetails = word }
                                        )
                                    }
                                    NavigationTab.ARCADE -> {
                                        LexiArcadeScreen(
                                            context = this@MainActivity,
                                            repository = repository,
                                            prefs = prefs,
                                            onBackClick = { currentTab = NavigationTab.HOME }
                                        )
                                    }
                                    NavigationTab.PROGRESS -> {
                                        ProgressScreen(
                                            context = this@MainActivity,
                                            repository = repository,
                                            prefs = prefs
                                        )
                                    }
                                    NavigationTab.SETTINGS -> {
                                        SettingsScreen(
                                            context = this@MainActivity,
                                            prefs = prefs,
                                            repository = repository,
                                            onEnableWallpaperClick = { openWallpaperChooser() },
                                            onOpenStudioClick = { isStudioOpen = true },
                                            onThemeChange = { mode -> currentThemeMode = mode },
                                            onBackClick = { currentTab = NavigationTab.HOME }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openWallpaperChooser() {
        WallpaperHelper.openWallpaperPicker(this)
    }
}