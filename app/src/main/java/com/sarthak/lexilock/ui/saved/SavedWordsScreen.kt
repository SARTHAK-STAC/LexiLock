package com.sarthak.lexilock.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.*
import com.sarthak.lexilock.ui.theme.*

@Composable
fun SavedWordsScreen(
    repository: WordRepository,
    onWordClick: (WordItem) -> Unit,
    onExploreClick: () -> Unit
) {
    var savedWords by remember { mutableStateOf(repository.getSavedWords()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredWords = remember(savedWords, searchQuery) {
        if (searchQuery.isBlank()) savedWords
        else savedWords.filter {
            it.word.contains(searchQuery.trim(), ignoreCase = true) ||
            it.definition.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(top = 20.dp, start = 24.dp, end = 24.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved",
                color = LexiDarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (savedWords.isNotEmpty()) {
                TextButton(
                    onClick = {
                        repository.clearAllSavedWords()
                        savedWords = emptyList()
                    }
                ) {
                    Text("Clear all", color = LexiDarkTextSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEARCH INPUT BAR (IF NON-EMPTY)
        if (savedWords.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = LexiDarkSurfaceSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, LexiDarkDivider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search saved words...", color = LexiDarkTextSecondary, fontSize = 14.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = LexiDarkTextPrimary,
                            unfocusedTextColor = LexiDarkTextPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            color = LexiDarkTextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LexiDivider(color = LexiDarkDivider)
        }

        // EMPTY STATE OR SIMPLE LIST ROWS
        if (savedWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No saved words yet",
                        color = LexiDarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the heart icon on any word to bookmark it.",
                        color = LexiDarkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    LexiButton(
                        text = "Explore words",
                        onClick = onExploreClick,
                        isPrimary = false
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredWords, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWordClick(item) }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.word.replaceFirstChar { it.uppercase() },
                                    color = LexiDarkTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.partOfSpeech.lowercase(),
                                    color = LexiDarkTextSecondary,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.definition,
                                color = LexiDarkTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 2,
                                lineHeight = 19.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Simple Outline Heart Toggle
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    repository.toggleSave(item.id, false)
                                    savedWords = repository.getSavedWords()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LexiIcons.Heart(
                                modifier = Modifier.size(18.dp),
                                tint = LexiDarkTextPrimary,
                                filled = true
                            )
                        }
                    }
                    LexiDivider(color = LexiDarkDivider)
                }
            }
        }
    }
}
