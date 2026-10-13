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

@Composable
fun WordLadderGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var currentRungIndex by remember { mutableIntStateOf(0) } // 0 to 7
    var livesRemaining by remember { mutableIntStateOf(3) }
    var currentBaseWord by remember { mutableStateOf<WordItem?>(null) }
    var currentOptions by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var correctTargetWord by remember { mutableStateOf<WordItem?>(null) }
    var selectedWordId by remember { mutableStateOf<Long?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }

    val allWords = remember { repository.getAllWords() }

    fun loadNextRung(previousWord: WordItem? = null) {
        if (allWords.size >= 8) {
            // Find base word or continuation
            val base = previousWord ?: allWords.random()
            // Look for synonyms or same category
            val poolWithSynonyms = allWords.filter { it.id != base.id && (it.category == base.category || it.synonyms.any { s -> base.synonyms.contains(s) }) }
            val closest = if (poolWithSynonyms.isNotEmpty()) poolWithSynonyms.random() else allWords.filter { it.id != base.id }.random()
            val distractors = allWords.filter { it.id != base.id && it.id != closest.id && it.category != closest.category }.shuffled().take(3)
            val options = (distractors + closest).shuffled()

            currentBaseWord = base
            correctTargetWord = closest
            currentOptions = options
            selectedWordId = null
        }
    }

    LaunchedEffect(Unit) {
        loadNextRung()
    }

    fun finishGame() {
        if (gameResult == null) {
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.WORD_LADDER,
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
                currentRungIndex = 0
                livesRemaining = 3
                score = 0
                combo = 0
                maxCombo = 0
                correctCount = 0
                incorrectCount = 0
                totalXpEarned = 0
                loadNextRung()
            },
            onBackToArcade = onBackClick
        )
        return
    }

    val base = currentBaseWord ?: return
    val target = correctTargetWord ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GameHeader(
            title = "Word Ladder",
            score = score,
            combo = combo,
            remainingSeconds = null,
            onExitClick = onBackClick
        )

        // RUNG PROGRESS & LIVES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUNG ${currentRungIndex + 1} OF 8",
                color = LexiDarkAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Row {
                repeat(3) { i ->
                    val isAlive = i < livesRemaining
                    Text(
                        text = if (isAlive) "♥" else "♡",
                        color = if (isAlive) Color(0xFFEF5350) else LexiDarkDivider,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Current word in chain:",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // BASE WORD CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = base.word.replaceFirstChar { it.uppercase() },
                    color = LexiDarkTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${base.partOfSpeech.lowercase()} • ${base.category}",
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Which word connects closest in meaning?",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // OPTIONS
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

                            if (currentRungIndex + 1 >= 8) {
                                finishGame()
                            }
                        } else {
                            incorrectCount += 1
                            combo = 0
                            livesRemaining -= 1
                            if (livesRemaining <= 0) {
                                finishGame()
                            }
                        }
                    }
                    .padding(14.dp)
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
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (selectedWordId != null && livesRemaining > 0 && currentRungIndex + 1 < 8) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = {
                    currentRungIndex += 1
                    loadNextRung(target)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = "Climb Next Rung →", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
