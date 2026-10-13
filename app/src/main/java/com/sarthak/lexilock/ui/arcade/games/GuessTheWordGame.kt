package com.sarthak.lexilock.ui.arcade.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sarthak.lexilock.data.model.GameResultData
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.MiniGameManager
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.arcade.GameHeader
import com.sarthak.lexilock.ui.arcade.GameResultScreen
import com.sarthak.lexilock.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GuessTheWordGame(
    repository: WordRepository,
    prefs: LexiPreferences,
    onExitGame: () -> Unit
) {
    val gameManager = remember { MiniGameManager(repository.contextForRepository()) }
    var timeLeft by remember { mutableIntStateOf(60) }
    var score by remember { mutableIntStateOf(0) }
    var currentCombo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempted by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var targetWord by remember { mutableStateOf<WordItem?>(null) }
    var clueLevel by remember { mutableIntStateOf(1) } // 1: Category, 2: Definition, 3: Context, 4: Synonyms, 5: First Letter
    var candidateOptions by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var selectedOptionId by remember { mutableStateOf<Long?>(null) }
    var isAnswerEvaluated by remember { mutableStateOf(false) }

    fun loadNewRound() {
        val all = repository.getAllWords().shuffled()
        if (all.size < 4) return
        val target = all.first()
        val distractors = all.filter { it.id != target.id }.take(3)
        val options = (distractors + target).shuffled()

        targetWord = target
        candidateOptions = options
        clueLevel = 1
        selectedOptionId = null
        isAnswerEvaluated = false
    }

    LaunchedEffect(Unit) {
        loadNewRound()
    }

    // Timer
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            }
            isGameOver = true
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.GUESS_THE_WORD,
                correctCount = correctCount,
                incorrectCount = totalAttempted - correctCount,
                totalXpAccumulated = totalXpEarned,
                maxCombo = maxCombo
            )
        }
    }

    if (isGameOver && gameResult != null) {
        GameResultScreen(
            result = gameResult!!,
            onPlayAgain = {
                timeLeft = 60
                score = 0
                currentCombo = 0
                maxCombo = 0
                correctCount = 0
                totalAttempted = 0
                totalXpEarned = 0
                isGameOver = false
                gameResult = null
                loadNewRound()
            },
            onBackToArcade = onExitGame
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        GameHeader(
            title = "Guess the Word",
            score = score,
            combo = currentCombo,
            remainingSeconds = timeLeft,
            onExitClick = onExitGame
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "“I'm thinking of a word...”",
            color = LexiDarkTextSecondary,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Clue Display Box
        targetWord?.let { word ->
            val potentialPts = when (clueLevel) {
                1 -> 100
                2 -> 80
                3 -> 60
                4 -> 40
                else -> 20
            }

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
                        text = "CLUE $clueLevel OF 5",
                        color = LexiDarkAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Worth $potentialPts pts",
                        color = LexiDarkTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clue 1: Category & Part of Speech
                Text(
                    text = "Category: ${word.category} (${word.partOfSpeech})",
                    color = LexiDarkTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                if (clueLevel >= 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Definition: ${word.definition}",
                        color = LexiDarkTextPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif
                    )
                }

                if (clueLevel >= 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val masked = word.exampleSentence.replace(
                        Regex(Regex.escape(word.word), RegexOption.IGNORE_CASE),
                        "[ _____ ]"
                    )
                    Text(
                        text = "Context: “${masked}”",
                        color = LexiDarkTextSecondary,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                if (clueLevel >= 4 && word.synonyms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Synonyms: ${word.synonyms.joinToString(", ")}",
                        color = LexiDarkTextPrimary,
                        fontSize = 14.sp
                    )
                }

                if (clueLevel >= 5) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val hint = "${word.word.first().uppercaseChar()}" + " _".repeat((word.word.length - 1).coerceAtLeast(0))
                    Text(
                        text = "First letter: $hint (${word.word.length} letters)",
                        color = LexiDarkAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (clueLevel < 5 && !isAnswerEvaluated) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LexiDarkDivider)
                            .clickable { clueLevel += 1 }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Need another clue? (-20 pts) →",
                            color = LexiDarkTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SELECT THE WORD",
            color = LexiDarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Options Grid
        candidateOptions.forEach { opt ->
            val isSelected = selectedOptionId == opt.id
            val isTarget = opt.id == targetWord?.id

            val borderColor = when {
                !isAnswerEvaluated -> if (isSelected) LexiDarkAccent else LexiDarkDivider
                isTarget -> LexiCorrectGreen
                isSelected -> LexiErrorRed
                else -> LexiDarkDivider
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable(enabled = !isAnswerEvaluated) {
                        selectedOptionId = opt.id
                        isAnswerEvaluated = true
                        totalAttempted += 1

                        if (isTarget) {
                            currentCombo += 1
                            if (currentCombo > maxCombo) maxCombo = currentCombo
                            correctCount += 1

                            val basePts = when (clueLevel) {
                                1 -> 100
                                2 -> 80
                                3 -> 60
                                4 -> 40
                                else -> 20
                            }
                            val gainedXp = gameManager.calculateComboXp(currentCombo)
                            totalXpEarned += gainedXp
                            score += basePts + (currentCombo * 5)

                            repository.recordGameWordCorrect(opt.id)
                        } else {
                            currentCombo = 0
                        }
                    }
                    .padding(16.dp)
            ) {
                Text(
                    text = opt.word,
                    color = LexiDarkTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isAnswerEvaluated) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexiDarkAccent)
                    .clickable { loadNewRound() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Next Word →",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Extension to retrieve context cleanly from repository
fun WordRepository.contextForRepository(): android.content.Context {
    return this.context
}
