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
fun DefinitionDuelGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var currentTargetWord by remember { mutableStateOf<WordItem?>(null) }
    var currentOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(45) }

    val allWords = remember { repository.getAllWords() }

    fun loadNextQuestion() {
        if (allWords.size >= 4) {
            val target = allWords.random()
            val distractors = allWords.filter { it.id != target.id }.shuffled().take(3).map { it.definition }
            val options = (distractors + target.definition).shuffled()

            currentTargetWord = target
            currentOptions = options
            selectedOption = null
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
                gameType = MiniGameType.DEFINITION_DUEL,
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
                remainingSeconds = 45
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
            title = "Definition Duel",
            score = score,
            combo = combo,
            remainingSeconds = remainingSeconds,
            onExitClick = onBackClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose the single true definition under speed:",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // WORD HERO CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = target.word.uppercase(),
                    color = LexiDarkAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = target.partOfSpeech.lowercase(),
                    color = LexiDarkTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        currentOptions.forEach { optionText ->
            val isSelected = (selectedOption == optionText)
            val isAnswer = (optionText == target.definition)

            val bgColor = when {
                selectedOption != null && isAnswer -> Color(0xFF2E3A2F)
                selectedOption != null && isSelected && !isAnswer -> Color(0xFF3E2723)
                else -> LexiDarkSurfaceSubtle
            }
            val borderColor = when {
                selectedOption != null && isAnswer -> Color(0xFF66BB6A)
                selectedOption != null && isSelected && !isAnswer -> Color(0xFFEF5350)
                else -> LexiDarkDivider
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = selectedOption == null) {
                        selectedOption = optionText
                        val isRight = (optionText == target.definition)

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
                    .padding(14.dp)
            ) {
                Text(
                    text = optionText,
                    color = if (selectedOption != null && isAnswer) Color(0xFFA5D6A7) else LexiDarkTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }
        }

        if (selectedOption != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = { loadNextQuestion() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = "Next Duel →", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
