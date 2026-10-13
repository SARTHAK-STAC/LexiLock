package com.sarthak.lexilock.ui.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*

@Composable
fun OnboardingScreen(
    context: Context,
    prefs: LexiPreferences,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedLevel by remember { mutableStateOf(prefs.englishLevel) }
    val selectedGoals = remember { mutableStateListOf<String>().apply { addAll(prefs.learningGoals) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP STEP COUNTER
        Text(
            text = "$step of 4",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        // MAIN EDITORIAL STEP BODY
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            when (step) {
                1 -> {
                    Column {
                        Text(
                            text = "Learn one word.\nEvery time you unlock.",
                            color = LexiDarkTextPrimary,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Effortless, daily vocabulary mastery built directly into your routine.",
                            color = LexiDarkTextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "What's your English level?",
                            color = LexiDarkTextPrimary,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        val levels = listOf("Beginner", "Intermediate", "Advanced", "Expert")
                        levels.forEach { lvl ->
                            val isSel = selectedLevel.equals(lvl, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLevel = lvl.lowercase() }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lvl,
                                    color = if (isSel) LexiDarkTextPrimary else LexiDarkTextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal
                                )
                                if (isSel) {
                                    Text(text = "✓", color = LexiDarkTextPrimary, fontSize = 16.sp)
                                }
                            }
                            LexiDivider(color = LexiDarkDivider)
                        }
                    }
                }
                3 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "What do you want to improve?",
                            color = LexiDarkTextPrimary,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        val goals = listOf(
                            "Vocabulary",
                            "Speaking",
                            "Writing",
                            "Academic English"
                        )
                        goals.forEach { g ->
                            val isSel = selectedGoals.contains(g)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSel) {
                                            if (selectedGoals.size > 1) selectedGoals.remove(g)
                                        } else {
                                            selectedGoals.add(g)
                                        }
                                    }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = g,
                                    color = if (isSel) LexiDarkTextPrimary else LexiDarkTextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal
                                )
                                if (isSel) {
                                    Text(text = "✓", color = LexiDarkTextPrimary, fontSize = 16.sp)
                                }
                            }
                            LexiDivider(color = LexiDarkDivider)
                        }
                    }
                }
                4 -> {
                    Column {
                        Text(
                            text = "Put LexiLock on your lock screen.",
                            color = LexiDarkTextPrimary,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Displays a beautifully typeset word on your lock screen through Android Live Wallpaper.",
                            color = LexiDarkTextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // BOTTOM CTA BUTTON
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    when (step) {
                        1 -> step = 2
                        2 -> {
                            prefs.englishLevel = selectedLevel
                            step = 3
                        }
                        3 -> {
                            prefs.learningGoals = selectedGoals.toSet()
                            step = 4
                        }
                        4 -> {
                            prefs.isOnboardingCompleted = true
                            onComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LexiDarkTextPrimary,
                    contentColor = LexiDarkBg
                )
            ) {
                Text(
                    text = when (step) {
                        4 -> "Set up wallpaper"
                        else -> "Continue"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (step > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { step-- },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Back", color = LexiDarkTextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
