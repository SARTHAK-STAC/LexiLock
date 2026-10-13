package com.sarthak.lexilock.ui.arcade.games

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.sarthak.lexilock.data.model.GameResultData
import com.sarthak.lexilock.data.model.MiniGameType
import com.sarthak.lexilock.data.repository.MiniGameManager
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.arcade.GameHeader
import com.sarthak.lexilock.ui.arcade.GameResultScreen
import com.sarthak.lexilock.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FillTheGapGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var currentTargetWord by remember { mutableStateOf<WordItem?>(null) }
    var maskedSentence by remember { mutableStateOf("") }
    var currentOptions by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var selectedWordId by remember { mutableStateOf<Long?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(60) }

    val allWords = remember { repository.getAllWords() }

    fun loadNextQuestion() {
        if (allWords.size >= 4) {
            // Find a target word whose example contains the word itself (case-insensitive)
            val candidate = allWords.filter { item ->
                item.exampleSentence.contains(item.word, ignoreCase = true)
            }.shuffled().firstOrNull() ?: allWords.random()

            val regex = Regex("(?i)\\b${Regex.escape(candidate.word)}\\b")
            val masked = if (regex.containsMatchIn(candidate.exampleSentence)) {
                candidate.exampleSentence.replace(regex, "_______")
            } else {
                "${candidate.exampleSentence} (_______)"
            }

            val distractors = allWords.filter { it.id != candidate.id }.shuffled().take(3)
            val options = (distractors + candidate).shuffled()

            currentTargetWord = candidate
            maskedSentence = masked
            currentOptions = options
            selectedWordId = null
        }
    }

    LaunchedEffect(Unit) {
        loadNextQuestion()
        while (remainingSeconds > 0 && gameResult == null) {
            delay(1000L)
            remainingSeconds -= 1
        }
        if (gameResult == null) {
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.FILL_THE_GAP,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                totalXpAccumulated = totalXpEarned,
                maxCombo = maxCombo
            )
        }
    }

    if (gameResult != null) {
        GameResultScreen(
            result = gameResult!!,
            onPlayAgain = {
                gameResult = null
                score = 0
                combo = 0
                maxCombo = 0
                correctCount = 0
                incorrectCount = 0
                totalXpEarned = 0
                remainingSeconds = 60
                loadNextQuestion()
            },
            onBackToArcade = onBackClick
        )
        return
    }

    val target = currentTargetWord ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GameHeader(
            title = "Fill the Gap",
            score = score,
            combo = combo,
            remainingSeconds = remainingSeconds,
            onExitClick = onBackClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Complete the sentence:",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // MASKED SENTENCE CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "“$maskedSentence”",
                color = LexiDarkTextPrimary,
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Choose the missing vocabulary word:",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // OPTIONS GRID / LIST
        currentOptions.forEach { option ->
            val isSelected = (selectedWordId == option.id)
            val isAnswer = (option.id == target.id)

            val bgColor = when {
                selectedWordId != null && isAnswer -> Color(0xFF2E3A2F)
                selectedWordId != null && isSelected && !isAnswer -> Color(0xFF3E2723)
                else -> LexiDarkSurfaceSubtle
            }
            val borderColor = when {
                selectedWordId != null && isAnswer -> Color(0xFF66BB6A)
                selectedWordId != null && isSelected && !isAnswer -> Color(0xFFEF5350)
                else -> LexiDarkDivider
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = selectedWordId == null) {
                        selectedWordId = option.id
                        val isRight = (option.id == target.id)

                        if (isRight) {
                            correctCount += 1
                            combo += 1
                            if (combo > maxCombo) maxCombo = combo
                            val gainedXp = gameManager.calculateComboXp(combo)
                            score += gainedXp
                            totalXpEarned += gainedXp
                            gameManager.onWordAnsweredCorrectly(target.id)
                        } else {
                            incorrectCount += 1
                            combo = 0
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.word.replaceFirstChar { it.uppercase() },
                        color = if (selectedWordId != null && isAnswer) Color(0xFFA5D6A7) else LexiDarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif
                    )

                    Text(
                        text = option.partOfSpeech.lowercase(),
                        color = LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        // NEXT QUESTION BUTTON IF ANSWERED
        if (selectedWordId != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = { loadNextQuestion() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = "Next Sentence →", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
