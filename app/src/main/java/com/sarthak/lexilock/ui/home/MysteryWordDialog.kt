package com.sarthak.lexilock.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.MysteryWordManager
import com.sarthak.lexilock.data.repository.MysteryWordState
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*
import com.sarthak.lexilock.utils.ShareCardGenerator

@Composable
fun MysteryWordDialog(
    context: Context,
    prefs: LexiPreferences,
    repository: WordRepository,
    onDismissRequest: () -> Unit
) {
    val mysteryManager = remember { MysteryWordManager(prefs, repository) }
    var mysteryState by remember { mutableStateOf(mysteryManager.getTodayState()) }
    var inputGuess by remember { mutableStateOf("") }
    var guessFeedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = LexiDarkBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, LexiDarkDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S MYSTERY WORD",
                            color = LexiDarkAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (mysteryState.isSolved) "Solved" else "Clue ${mysteryState.cluesRevealed} of 4",
                            color = LexiDarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "✕",
                        color = LexiDarkTextSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable { onDismissRequest() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (mysteryState.isSolved) {
                    // SOLVED STATE
                    val word = mysteryState.wordItem
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LexiDarkSurfaceSubtle)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = word.word.replaceFirstChar { it.uppercase() },
                            color = LexiDarkTextPrimary,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${word.partOfSpeech}  •  ${word.pronunciation}",
                            color = LexiDarkAccent,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = word.definition,
                            color = LexiDarkTextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "“${word.exampleSentence}”",
                            color = LexiDarkTextSecondary,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "+${mysteryState.earnedXp} XP Awarded",
                            color = LexiCorrectGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons: Save & Share Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentSaved = mysteryState.wordItem.isSaved
                                repository.toggleSave(mysteryState.wordItem.id, !currentSaved)
                                mysteryState = mysteryState.copy(
                                    wordItem = mysteryState.wordItem.copy(isSaved = !currentSaved)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkSurfaceSubtle),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (mysteryState.wordItem.isSaved) "Saved ✓" else "Save Word",
                                color = LexiDarkTextPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                ShareCardGenerator.shareMysteryWord(
                                    context = context,
                                    wordItem = mysteryState.wordItem,
                                    cluesUsed = mysteryState.cluesRevealed,
                                    xpEarned = mysteryState.earnedXp
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Share Card ↗",
                                color = LexiDarkBg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // PROGRESSIVE CLUES DISCLOSURE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LexiDarkSurfaceSubtle)
                            .padding(16.dp)
                    ) {
                        // Clue 1: Part of Speech & Category
                        Text(
                            text = "Clue 1: Part of speech",
                            color = LexiDarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${mysteryState.wordItem.partOfSpeech.replaceFirstChar { it.uppercase() }} in category “${mysteryState.wordItem.category}”",
                            color = LexiDarkTextPrimary,
                            fontSize = 14.sp
                        )

                        // Clue 2: Definition
                        if (mysteryState.cluesRevealed >= 2) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LexiDivider(color = LexiDarkDivider)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clue 2: Definition",
                                color = LexiDarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mysteryState.wordItem.definition,
                                color = LexiDarkTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        // Clue 3: Context Sentence
                        if (mysteryState.cluesRevealed >= 3) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LexiDivider(color = LexiDarkDivider)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clue 3: Context",
                                color = LexiDarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "“${mysteryState.maskedExample}”",
                                color = LexiDarkTextSecondary,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        // Clue 4: Letter hint
                        if (mysteryState.cluesRevealed >= 4) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LexiDivider(color = LexiDarkDivider)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clue 4: Letter Pattern",
                                color = LexiDarkAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mysteryState.letterHint,
                                color = LexiDarkTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Guess Input Box
                    OutlinedTextField(
                        value = inputGuess,
                        onValueChange = {
                            inputGuess = it
                            guessFeedback = null
                        },
                        placeholder = { Text("Type your guess...", color = LexiDarkTextSecondary, fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LexiDarkAccent,
                            unfocusedBorderColor = LexiDarkDivider,
                            focusedTextColor = LexiDarkTextPrimary,
                            unfocusedTextColor = LexiDarkTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (guessFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = guessFeedback!!,
                            color = if (isError) LexiErrorRed else LexiCorrectGreen,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Guess action button
                    Button(
                        onClick = {
                            if (inputGuess.isBlank()) return@Button
                            val (correct, newState) = mysteryManager.attemptGuess(inputGuess)
                            mysteryState = newState
                            if (correct) {
                                isError = false
                                guessFeedback = "Correct! You cracked the mystery word."
                            } else {
                                isError = true
                                guessFeedback = "Not quite. Try another guess or reveal a clue."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Submit Guess",
                            color = LexiDarkBg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary actions: Reveal Next Clue or Give Up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mysteryState.cluesRevealed < 4) {
                            Text(
                                text = "Reveal Clue ${mysteryState.cluesRevealed + 1} →",
                                color = LexiDarkTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable {
                                        mysteryState = mysteryManager.revealNextClue()
                                    }
                                    .padding(4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Text(
                            text = "Reveal Word (Surrender)",
                            color = LexiDarkTextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable {
                                    mysteryState = mysteryManager.revealWord()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
