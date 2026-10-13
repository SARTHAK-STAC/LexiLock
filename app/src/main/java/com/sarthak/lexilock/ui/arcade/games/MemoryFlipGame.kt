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

private data class MemoryCard(
    val id: Int,
    val wordId: Long,
    val text: String,
    val isWord: Boolean
)

@Composable
fun MemoryFlipGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var cards by remember { mutableStateOf<List<MemoryCard>>(emptyList()) }
    var revealedCardIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var matchedWordIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedFirstCardId by remember { mutableStateOf<Int?>(null) }
    var isCheckingMatch by remember { mutableStateOf(false) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(60) }

    val allWords = remember { repository.getAllWords() }

    fun loadNewBoard() {
        if (allWords.size >= 4) {
            val selectedWords = allWords.shuffled().take(4) // 4 pairs = 8 cards
            val cardList = mutableListOf<MemoryCard>()
            var nextId = 0
            selectedWords.forEach { item ->
                cardList.add(MemoryCard(nextId++, item.id, item.word.replaceFirstChar { it.uppercase() }, isWord = true))
                cardList.add(MemoryCard(nextId++, item.id, item.definition, isWord = false))
            }
            cards = cardList.shuffled()
            revealedCardIds = emptySet()
            matchedWordIds = emptySet()
            selectedFirstCardId = null
            isCheckingMatch = false
        }
    }

    LaunchedEffect(Unit) {
        loadNewBoard()
        while (remainingSeconds > 0 && gameResult == null) {
            delay(1000L)
            remainingSeconds -= 1
        }
        if (gameResult == null) {
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.MEMORY_FLIP,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                totalXpAccumulated = totalXpEarned,
                maxCombo = maxCombo
            )
        }
    }

    // Check board completion
    LaunchedEffect(matchedWordIds) {
        if (matchedWordIds.size == 4) {
            delay(500L)
            loadNewBoard()
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
                loadNewBoard()
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
            .verticalScroll(rememberScrollState())
    ) {
        GameHeader(
            title = "Memory Flip",
            score = score,
            combo = combo,
            remainingSeconds = remainingSeconds,
            onExitClick = onBackClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Flip cards to find pairs of words and their definitions:",
            color = LexiDarkTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4 ROWS x 2 COLUMNS MEMORY GRID
        val chunkedCards = cards.chunked(2)
        chunkedCards.forEach { rowCards ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCards.forEach { card ->
                    val isRevealed = revealedCardIds.contains(card.id)
                    val isMatched = matchedWordIds.contains(card.wordId)

                    val bgColor = when {
                        isMatched -> Color(0xFF2E3A2F)
                        isRevealed -> LexiDarkSurfaceSubtle
                        else -> LexiDarkBg
                    }
                    val borderColor = when {
                        isMatched -> Color(0xFF66BB6A)
                        isRevealed -> LexiDarkAccent
                        else -> LexiDarkDivider
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isRevealed && !isMatched && !isCheckingMatch) {
                                if (selectedFirstCardId == null) {
                                    // First card flipped
                                    selectedFirstCardId = card.id
                                    revealedCardIds = revealedCardIds + card.id
                                } else {
                                    // Second card flipped
                                    val firstId = selectedFirstCardId!!
                                    val firstCard = cards.firstOrNull { it.id == firstId }
                                    revealedCardIds = revealedCardIds + card.id
                                    selectedFirstCardId = null

                                    if (firstCard != null && firstCard.wordId == card.wordId && firstCard.isWord != card.isWord) {
                                        // MATCH!
                                        matchedWordIds = matchedWordIds + card.wordId
                                        correctCount += 1
                                        combo += 1
                                        if (combo > maxCombo) maxCombo = combo
                                        val gainedXp = gameManager.calculateComboXp(combo)
                                        score += gainedXp
                                        totalXpEarned += gainedXp
                                        gameManager.onWordAnsweredCorrectly(card.wordId)
                                    } else {
                                        // NO MATCH - Delay then flip back
                                        incorrectCount += 1
                                        combo = 0
                                        isCheckingMatch = true
                                    }
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRevealed || isMatched) {
                            Text(
                                text = card.text,
                                color = if (isMatched) Color(0xFFA5D6A7) else LexiDarkTextPrimary,
                                fontSize = if (card.isWord) 16.sp else 12.sp,
                                fontWeight = if (card.isWord) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = if (card.isWord) FontFamily.Serif else FontFamily.Default,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 4
                            )
                        } else {
                            Text(
                                text = "◆",
                                color = LexiDarkTextSecondary.copy(alpha = 0.5f),
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isCheckingMatch) {
        if (isCheckingMatch) {
            delay(850L)
            revealedCardIds = emptySet()
            isCheckingMatch = false
        }
    }
}
