package com.sarthak.lexilock.ui.arcade

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.BattleChallenge
import com.sarthak.lexilock.data.repository.BattleShowdown
import com.sarthak.lexilock.data.repository.FriendBattleManager
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.LexiDivider
import com.sarthak.lexilock.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FriendBattleDialog(
    context: Context,
    prefs: LexiPreferences,
    repository: WordRepository,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Create Challenge, 1: Enter Code

    // Create flow state
    var questionCountChoice by remember { mutableIntStateOf(5) } // 5 or 10 questions
    var isCreateGameActive by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var battleWords by remember { mutableStateOf<List<WordItem>>(emptyList()) }

    // Solve flow state
    var inputCode by remember { mutableStateOf("") }
    var parsedChallenge by remember { mutableStateOf<BattleChallenge?>(null) }
    var isSolveGameActive by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // Active 5-question match state
    var questionIndex by remember { mutableIntStateOf(0) }
    var currentScore by remember { mutableIntStateOf(0) }
    var matchDone by remember { mutableStateOf(false) }
    var showdownResult by remember { mutableStateOf<BattleShowdown?>(null) }

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
                            text = "FRIEND WORD BATTLE",
                            color = LexiDarkAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "5-Word Vocabulary Showdown",
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

                if (isCreateGameActive || isSolveGameActive) {
                    // ACTIVE MATCH RUNNER (5 questions)
                    val currentWord = battleWords.getOrNull(questionIndex)
                    if (currentWord != null && !matchDone) {
                        var options by remember(questionIndex) {
                            val distractors = repository.getAllWords()
                                .filter { it.id != currentWord.id }
                                .shuffled()
                                .take(3)
                            mutableStateOf((distractors + currentWord).shuffled())
                        }
                        var selectedOption by remember(questionIndex) { mutableStateOf<Long?>(null) }
                        var isAnswered by remember(questionIndex) { mutableStateOf(false) }

                        val totalQuestions = battleWords.size
                        Text(
                            text = "QUESTION ${questionIndex + 1} OF $totalQuestions",
                            color = LexiDarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“${currentWord.definition}”",
                            color = LexiDarkTextPrimary,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        options.forEach { opt ->
                            val isChosen = selectedOption == opt.id
                            val isTarget = opt.id == currentWord.id
                            val bColor = when {
                                !isAnswered -> if (isChosen) LexiDarkAccent else LexiDarkDivider
                                isTarget -> LexiCorrectGreen
                                isChosen -> LexiErrorRed
                                else -> LexiDarkDivider
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LexiDarkSurfaceSubtle)
                                    .clickable(enabled = !isAnswered) {
                                        selectedOption = opt.id
                                        isAnswered = true
                                        if (isTarget) {
                                            currentScore += 100
                                            com.sarthak.lexilock.utils.LexiSoundHapticsManager.performHaptic(context, com.sarthak.lexilock.utils.FeedbackType.CORRECT)
                                            com.sarthak.lexilock.utils.LexiSoundHapticsManager.performSound(context, com.sarthak.lexilock.utils.FeedbackType.CORRECT)
                                        } else {
                                            com.sarthak.lexilock.utils.LexiSoundHapticsManager.performHaptic(context, com.sarthak.lexilock.utils.FeedbackType.ERROR)
                                            com.sarthak.lexilock.utils.LexiSoundHapticsManager.performSound(context, com.sarthak.lexilock.utils.FeedbackType.ERROR)
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = opt.word,
                                    color = if (isAnswered && isTarget) LexiCorrectGreen else LexiDarkTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isAnswered) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (questionIndex < totalQuestions - 1) {
                                        questionIndex += 1
                                    } else {
                                        matchDone = true
                                        if (isCreateGameActive) {
                                            generatedCode = FriendBattleManager.createChallengeCode(
                                                userName = "Sarthak",
                                                score = currentScore,
                                                words = battleWords
                                            )
                                        } else if (parsedChallenge != null) {
                                            showdownResult = FriendBattleManager.compareResult(
                                                challenge = parsedChallenge!!,
                                                userScore = currentScore
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (questionIndex < totalQuestions - 1) "Next Question →" else "Finish Match",
                                    color = LexiDarkBg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (matchDone) {
                        // MATCH COMPLETE REPORT
                        if (isCreateGameActive && generatedCode != null) {
                            val totalPossible = battleWords.size * 100
                            Text(
                                text = "BATTLE CREATED",
                                color = LexiDarkAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your Score: $currentScore / $totalPossible",
                                color = LexiDarkTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Challenge code generated. Share with a friend so they face the exact same ${battleWords.size} words.",
                                color = LexiDarkTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LexiDarkSurfaceSubtle)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = generatedCode!!,
                                    color = LexiDarkAccent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val shareMessage = "Challenge me on LexiLock.\n\n${battleWords.size} vocabulary questions.\nCan you beat my score?\n\nOpen LexiLock and enter:\n${generatedCode!!}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Battle"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "SHARE BATTLE ↗", color = LexiDarkBg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (showdownResult != null) {
                            val res = showdownResult!!
                            Text(
                                text = if (res.isUserWinner) "YOU WIN 🏆" else if (res.isDraw) "TIED MATCH 🤝" else "CHALLENGER WON ⚔️",
                                color = if (res.isUserWinner) LexiCorrectGreen else LexiDarkTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LexiDarkSurfaceSubtle)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "You", color = LexiDarkTextSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${res.userScore}", color = LexiDarkAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LexiDarkDivider))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = res.challengerName, color = LexiDarkTextSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${res.challengerScore}", color = LexiDarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val outcome = if (res.isUserWinner) "I defeated" else if (res.isDraw) "Tied against" else "Lost against"
                                    val shareMessage = "LexiLock Word Battle Showdown\n\n$outcome ${res.challengerName}!\nScore: ${res.userScore} vs ${res.challengerScore}\n\nCan you beat my vocabulary score?\nLearn a word every day with LexiLock."
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Battle Result"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "SHARE SHOWDOWN ↗", color = LexiDarkBg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onDismissRequest,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LexiDarkTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LexiDarkDivider),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Done", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // TAB SELECTOR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LexiDarkSurfaceSubtle)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedTab == 0) LexiDarkBg else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create",
                                color = if (selectedTab == 0) LexiDarkTextPrimary else LexiDarkTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedTab == 1) LexiDarkBg else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Accept Code",
                                color = if (selectedTab == 1) LexiDarkTextPrimary else LexiDarkTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedTab == 0) {
                        // CREATE TAB
                        Text(
                            text = "Choose match length and generate a challenge code to test any friend on the exact same words.",
                            color = LexiDarkTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Length choice: 5 vs 10 questions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LexiDarkSurfaceSubtle)
                                .padding(4.dp)
                        ) {
                            listOf(5 to "5 Questions (~1 min)", 10 to "10 Questions (~2 mins)").forEach { (cnt, label) ->
                                val isSel = questionCountChoice == cnt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) LexiDarkBg else Color.Transparent)
                                        .clickable { questionCountChoice = cnt }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) LexiDarkAccent else LexiDarkTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                battleWords = repository.getAllWords().shuffled().take(questionCountChoice)
                                questionIndex = 0
                                currentScore = 0
                                matchDone = false
                                isCreateGameActive = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Start $questionCountChoice-Word Battle", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // ENTER CODE TAB
                        Text(
                            text = "Paste a friend's LEXI-BATTLE code below to face their exact vocabulary test.",
                            color = LexiDarkTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = {
                                inputCode = it
                                parseError = null
                            },
                            placeholder = { Text("LEXI-BATTLE:...", color = LexiDarkTextSecondary, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LexiDarkAccent,
                                unfocusedBorderColor = LexiDarkDivider,
                                focusedTextColor = LexiDarkTextPrimary,
                                unfocusedTextColor = LexiDarkTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (parseError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = parseError!!, color = LexiErrorRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val parsed = FriendBattleManager.parseChallengeCode(inputCode)
                                if (parsed == null) {
                                    parseError = "Invalid battle code format. Example: LEXI-BATTLE:..."
                                } else {
                                    val words = parsed.wordIds.mapNotNull { repository.getWordById(it) }
                                    if (words.size < 5) {
                                        parseError = "Could not load all words for this battle."
                                    } else {
                                        parsedChallenge = parsed
                                        battleWords = words
                                        questionIndex = 0
                                        currentScore = 0
                                        matchDone = false
                                        isSolveGameActive = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Accept Challenge", color = LexiDarkBg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
