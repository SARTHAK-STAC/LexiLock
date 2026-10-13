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
fun QuickMatchGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    // Game state
    var roundWords by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var shuffledDefinitions by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var selectedWordId by remember { mutableStateOf<Long?>(null) }
    var matchedWordIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var errorWordId by remember { mutableStateOf<Long?>(null) }
    var errorDefId by remember { mutableStateOf<Long?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(45) }

    fun loadNewRound() {
        val all = repository.getAllWords().shuffled().take(4)
        if (all.size >= 4) {
            roundWords = all
            shuffledDefinitions = all.map { it.id to it.definition }.shuffled()
            selectedWordId = null
            matchedWordIds = emptySet()
            errorWordId = null
            errorDefId = null
        }
    }

    LaunchedEffect(Unit) {
        loadNewRound()
        while (remainingSeconds > 0 && gameResult == null) {
            delay(1000L)
            remainingSeconds -= 1
        }
        if (gameResult == null) {
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.QUICK_MATCH,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                totalXpAccumulated = totalXpEarned,
                maxCombo = maxCombo
            )
        }
    }

    // Check round complete
    LaunchedEffect(matchedWordIds) {
        if (roundWords.isNotEmpty() && matchedWordIds.size == roundWords.size) {
            delay(400L)
            loadNewRound()
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
                loadNewRound()
            },
            onBackToArcade = onBackClick
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        GameHeader(
            title = "Quick Match",
            score = score,
            combo = combo,
            remainingSeconds = remainingSeconds,
            onExitClick = onBackClick
        )

        Text(
            text = "Tap a word, then tap its matching definition.",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // WORDS COLUMN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                roundWords.forEach { item ->
                    val isMatched = matchedWordIds.contains(item.id)
                    val isSelected = (selectedWordId == item.id)
                    val isError = (errorWordId == item.id)

                    val bgColor = when {
                        isMatched -> Color(0xFF2E3A2F)
                        isError -> Color(0xFF3E2723)
                        isSelected -> LexiDarkAccent.copy(alpha = 0.2f)
                        else -> LexiDarkSurfaceSubtle
                    }
                    val borderColor = when {
                        isMatched -> Color(0xFF66BB6A)
                        isError -> Color(0xFFEF5350)
                        isSelected -> LexiDarkAccent
                        else -> LexiDarkDivider
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isMatched) {
                                selectedWordId = item.id
                                errorWordId = null
                                errorDefId = null
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.word.replaceFirstChar { it.uppercase() },
                            color = if (isMatched) Color(0xFFA5D6A7) else LexiDarkTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }

            // DEFINITIONS COLUMN
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shuffledDefinitions.forEach { (wordId, defText) ->
                    val isMatched = matchedWordIds.contains(wordId)
                    val isError = (errorDefId == wordId)

                    val bgColor = when {
                        isMatched -> Color(0xFF2E3A2F)
                        isError -> Color(0xFF3E2723)
                        else -> LexiDarkSurfaceSubtle
                    }
                    val borderColor = when {
                        isMatched -> Color(0xFF66BB6A)
                        isError -> Color(0xFFEF5350)
                        else -> LexiDarkDivider
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isMatched && selectedWordId != null) {
                                val currentWordSel = selectedWordId
                                if (currentWordSel != null) {
                                    if (currentWordSel == wordId) {
                                        // CORRECT
                                        matchedWordIds = matchedWordIds + wordId
                                        selectedWordId = null
                                        correctCount += 1
                                        combo += 1
                                        if (combo > maxCombo) maxCombo = combo
                                        val gainedXp = gameManager.calculateComboXp(combo)
                                        score += gainedXp
                                        totalXpEarned += gainedXp
                                        gameManager.onWordAnsweredCorrectly(wordId)
                                    } else {
                                        // INCORRECT
                                        incorrectCount += 1
                                        combo = 0
                                        errorWordId = currentWordSel
                                        errorDefId = wordId
                                    }
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = defText,
                            color = if (isMatched) Color(0xFFA5D6A7) else LexiDarkTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
