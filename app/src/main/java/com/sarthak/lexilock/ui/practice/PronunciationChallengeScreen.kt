package com.sarthak.lexilock.ui.practice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.theme.*
import java.util.Locale

@Composable
fun PronunciationChallengeScreen(
    context: Context,
    repository: WordRepository,
    prefs: LexiPreferences,
    onBackClick: () -> Unit
) {
    var words by remember { mutableStateOf(repository.getAllWords().shuffled()) }
    var currentIndex by remember { mutableStateOf(0) }
    val currentWord = words.getOrNull(currentIndex)

    var isListening by remember { mutableStateOf(false) }
    var recognizedWord by remember { mutableStateOf<String?>(null) }
    var evaluationResult by remember { mutableStateOf<String?>(null) }
    var resultXp by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            errorMessage = null
        } else {
            errorMessage = "Microphone access is needed for speech comparison."
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Speech recognition is not available on this device."
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    errorMessage = null
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                        else -> "Recognition error ($error). Try again."
                    }
                    recognizer.destroy()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    recognizedWord = spoken

                    if (currentWord != null && spoken.isNotBlank()) {
                        val target = currentWord.word.trim().lowercase()
                        val heard = spoken.lowercase()

                        if (heard == target || heard.contains(target)) {
                            evaluationResult = "EXCELLENT"
                            resultXp = 20
                            prefs.addXp(20)
                            prefs.pronunciationCompletedCount += 1
                        } else {
                            val distance = levenshtein(target, heard)
                            if (distance <= 2) {
                                evaluationResult = "ALMOST THERE"
                                resultXp = 10
                                prefs.addXp(10)
                                prefs.pronunciationCompletedCount += 1
                            } else {
                                evaluationResult = "KEEP PRACTICING"
                                resultXp = 0
                            }
                        }
                    }
                    recognizer.destroy()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            errorMessage = e.message ?: "Failed to start speech recognition."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .padding(8.dp)
            ) {
                Text(text = "✕", color = LexiDarkTextSecondary, fontSize = 20.sp)
            }
            Text(
                text = "PRONUNCIATION LAB",
                color = LexiDarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(32.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (currentWord != null) {
            // Word Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentWord.word,
                    color = LexiDarkTextPrimary,
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${currentWord.partOfSpeech}  •  ${currentWord.pronunciation}",
                    color = LexiDarkAccent,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentWord.definition,
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Listen button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LexiDarkDivider)
                        .clickable {
                            tts?.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, "vocab_tts")
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🔊 Listen Native Audio",
                        color = LexiDarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Microphone Interaction
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isListening) LexiErrorRed else LexiDarkAccent)
                        .clickable(enabled = !isListening) {
                            recognizedWord = null
                            evaluationResult = null
                            errorMessage = null
                            startListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isListening) "🎙️" else "🎤",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isListening) "Listening... speak now" else "Tap microphone & speak word",
                    color = LexiDarkTextSecondary,
                    fontSize = 14.sp
                )
            }

            // Results Feedback
            if (evaluationResult != null) {
                Spacer(modifier = Modifier.height(24.dp))
                val isExcellent = evaluationResult == "EXCELLENT"
                val isAlmost = evaluationResult == "ALMOST THERE"

                val resultColor = when {
                    isExcellent -> LexiCorrectGreen
                    isAlmost -> LexiAmberWarning
                    else -> LexiErrorRed
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LexiDarkSurfaceSubtle)
                        .border(1.5.dp, resultColor, RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = evaluationResult!!,
                        color = resultColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Heard: “${recognizedWord ?: ""}”",
                        color = LexiDarkTextSecondary,
                        fontSize = 14.sp
                    )

                    if (resultXp > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+$resultXp XP Awarded",
                            color = LexiDarkAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = LexiErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Next Word Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LexiDarkAccent)
                    .clickable {
                        currentIndex = (currentIndex + 1) % words.size
                        recognizedWord = null
                        evaluationResult = null
                        errorMessage = null
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Next Practice Word →",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length
    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i
        for (j in 1..lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1
            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }
        val swap = cost
        cost = newCost
        newCost = swap
    }
    return cost[lhsLength]
}
