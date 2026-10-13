package com.sarthak.lexilock.ui.arcade.games

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
fun SpellItGame(
    context: Context,
    repository: WordRepository,
    onBackClick: () -> Unit
) {
    val gameManager = remember { MiniGameManager.getInstance(context) }
    var gameResult by remember { mutableStateOf<GameResultData?>(null) }

    var currentTargetWord by remember { mutableStateOf<WordItem?>(null) }
    var userSpellingInput by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var isAnswerCorrect by remember { mutableStateOf(false) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var incorrectCount by remember { mutableIntStateOf(0) }
    var totalXpEarned by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(60) }

    val allWords = remember { repository.getAllWords() }

    fun loadNextWord() {
        if (allWords.isNotEmpty()) {
            val target = allWords.random()
            currentTargetWord = target
            userSpellingInput = ""
            isSubmitted = false
            isAnswerCorrect = false
        }
    }

    LaunchedEffect(Unit) {
        loadNextWord()
        while (remainingSeconds > 0 && gameResult == null) {
            delay(1000L)
            remainingSeconds -= 1
        }
        if (gameResult == null) {
            gameResult = gameManager.recordGameFinished(
                gameType = MiniGameType.SPELL_IT,
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
                loadNextWord()
            },
            onBackToArcade = onBackClick
        )
        return
    }

    val target = currentTargetWord ?: return

    fun submitAnswer() {
        if (isSubmitted || userSpellingInput.isBlank()) return
        isSubmitted = true
        val isRight = userSpellingInput.trim().equals(target.word.trim(), ignoreCase = true)
        isAnswerCorrect = isRight

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GameHeader(
            title = "Spell It",
            score = score,
            combo = combo,
            remainingSeconds = remainingSeconds,
            onExitClick = onBackClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Definition:",
            color = LexiDarkTextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // DEFINITION CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LexiDarkSurfaceSubtle)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = target.definition,
                    color = LexiDarkTextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${target.partOfSpeech.lowercase()} • ${target.word.length} letters",
                    color = LexiDarkTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LETTER BLANKS PREVIEW
        val displayBlanks = target.word.mapIndexed { idx, char ->
            if (isSubmitted) {
                target.word[idx].uppercaseChar()
            } else if (idx < userSpellingInput.length) {
                userSpellingInput[idx].uppercaseChar()
            } else {
                '_'
            }
        }.joinToString(" ")

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayBlanks,
                color = if (isSubmitted) {
                    if (isAnswerCorrect) Color(0xFFA5D6A7) else Color(0xFFEF5350)
                } else LexiDarkAccent,
                fontSize = 26.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INPUT TEXT FIELD
        OutlinedTextField(
            value = userSpellingInput,
            onValueChange = { if (!isSubmitted && it.length <= target.word.length) userSpellingInput = it },
            placeholder = { Text("Type word spelling...", color = LexiDarkTextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
            enabled = !isSubmitted,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LexiDarkSurfaceSubtle,
                unfocusedContainerColor = LexiDarkSurfaceSubtle,
                focusedTextColor = LexiDarkTextPrimary,
                unfocusedTextColor = LexiDarkTextPrimary,
                focusedIndicatorColor = LexiDarkAccent,
                unfocusedIndicatorColor = LexiDarkDivider
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isSubmitted) {
            androidx.compose.material3.Button(
                onClick = { submitAnswer() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = "Submit Word", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // FEEDBACK & NEXT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAnswerCorrect) Color(0xFF2E3A2F) else Color(0xFF3E2723))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAnswerCorrect) "✓ Correct!" else "The correct spelling is: ${target.word.uppercase()}",
                    color = if (isAnswerCorrect) Color(0xFFA5D6A7) else Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.material3.Button(
                onClick = { loadNextWord() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = "Next Word →", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
