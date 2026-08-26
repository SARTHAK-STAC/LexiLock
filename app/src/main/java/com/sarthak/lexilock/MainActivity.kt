package com.sarthak.lexilock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.ui.theme.LexiLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LexiLockTheme {

                val context = LocalContext.current

                // Check if user specifically requested the quiz
                val openQuiz = intent.getBooleanExtra(
                    "OPEN_QUIZ",
                    false
                )

                var onboardingCompleted by remember {
                    mutableStateOf(
                        UserPreferences.isOnboardingCompleted(context)
                    )
                }

                // --------------------------------
                // HOME OR QUIZ
                // --------------------------------

                if (onboardingCompleted && !openQuiz) {

                    HomeScreen()

                } else {

                    QuizScreen(
                        onFinish = {

                            // Save onboarding status
                            UserPreferences.completeOnboarding(context)

                            // Open wallpaper picker
                            val wallpaperIntent = Intent(
                                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                            )

                            wallpaperIntent.putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(
                                    this,
                                    LexiWallpaperService::class.java
                                )
                            )

                            startActivity(wallpaperIntent)

                            onboardingCompleted = true
                        }
                    )
                }
            }
        }
    }
}


// =========================================
// HOME SCREEN
// =========================================

@Composable
fun HomeScreen() {

    val context = LocalContext.current

    val level = UserPreferences.getLevel(context)

    val todayWord = DailyWordManager.getTodayWord(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        // --------------------------------
        // APP NAME
        // --------------------------------

        Text(
            text = "LEXILOCK",
            fontSize = 28.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // --------------------------------
        // TODAY'S WORD
        // --------------------------------

        Text(
            text = "Today's Word",
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = todayWord.word,
            fontSize = 38.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = todayWord.pronunciation,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = todayWord.meaning,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // --------------------------------
        // USER LEVEL
        // --------------------------------

        Text(
            text = "Level: ${level.uppercase()}",
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // --------------------------------
        // SET WALLPAPER
        // --------------------------------

        Button(
            onClick = {

                val intent = Intent(
                    WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                )

                intent.putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(
                        context,
                        LexiWallpaperService::class.java
                    )
                )

                context.startActivity(intent)
            }
        ) {

            Text("Set Wallpaper")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // --------------------------------
        // RETAKE QUIZ
        // --------------------------------

        OutlinedButton(
            onClick = {

                val quizIntent = Intent(
                    context,
                    MainActivity::class.java
                )

                quizIntent.putExtra(
                    "OPEN_QUIZ",
                    true
                )

                context.startActivity(quizIntent)
            }
        ) {

            Text("Retake Quiz")
        }
    }
}


// =========================================
// QUIZ SCREEN
// =========================================

@Composable
fun QuizScreen(
    onFinish: () -> Unit
) {

    val context = LocalContext.current

    var currentQuestion by remember {
        mutableStateOf(0)
    }

    var score by remember {
        mutableStateOf(0)
    }

    var selectedAnswer by remember {
        mutableStateOf(-1)
    }

    val questions = QuizRepository.questions

    // =====================================
    // QUIZ FINISHED
    // =====================================

    if (currentQuestion == questions.size) {

        val level = LevelManager.calculateLevel(score)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            verticalArrangement = Arrangement.Center,

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Your Level",
                fontSize = 20.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = level.uppercase(),
                fontSize = 34.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Score: $score/${questions.size}",
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Button(
                onClick = {

                    // Save the new level
                    UserPreferences.saveLevel(
                        context = context,
                        level = level
                    )

                    // Finish onboarding / quiz
                    onFinish()
                }
            ) {

                Text("Set Today's Wallpaper")
            }
        }

        return
    }

    // =====================================
    // CURRENT QUESTION
    // =====================================

    val question = questions[currentQuestion]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // Question number

        Text(
            text = "Question ${currentQuestion + 1}/${questions.size}",
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Question

        Text(
            text = question.question,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =================================
        // ANSWER OPTIONS
        // =================================

        question.options.forEachIndexed { index, option ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .selectable(
                        selected = selectedAnswer == index,

                        onClick = {
                            selectedAnswer = index
                        }
                    )
            ) {

                Text(
                    text = option,

                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // =================================
        // NEXT BUTTON
        // =================================

        Button(
            modifier = Modifier.fillMaxWidth(),

            enabled = selectedAnswer != -1,

            onClick = {

                // Check answer
                if (selectedAnswer == question.correctAnswer) {
                    score++
                }

                // Next question
                currentQuestion++

                // Reset selection
                selectedAnswer = -1
            }
        ) {

            Text("Next")
        }
    }
}