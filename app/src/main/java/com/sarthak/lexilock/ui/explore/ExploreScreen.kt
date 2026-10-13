package com.sarthak.lexilock.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.sarthak.lexilock.data.repository.WordRepository
import com.sarthak.lexilock.domain.model.WordItem
import com.sarthak.lexilock.ui.components.*
import com.sarthak.lexilock.ui.theme.*

@Composable
fun ExploreScreen(
    repository: WordRepository,
    onWordClick: (WordItem) -> Unit
) {
    var selectedScopeTab by remember { mutableStateOf(0) } // 0: All Words, 1: Saved Words
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isReviewDialogOpen by remember { mutableStateOf(false) }
    var reviewWords by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var reviewIndex by remember { mutableIntStateOf(0) }

    val difficulties = listOf("All", "Beginner", "Intermediate", "Advanced", "Expert")
    val categories = listOf(
        "All",
        "Communication",
        "Personality",
        "Nature",
        "Business",
        "Academic",
        "Everyday English",
        "Technology",
        "Emotions",
        "Interviews"
    )

    val wordList by remember(searchQuery, selectedDifficulty, selectedCategory, selectedScopeTab) {
        derivedStateOf {
            if (selectedScopeTab == 1) {
                repository.getSavedWords().filter {
                    (searchQuery.isBlank() || it.word.contains(searchQuery, ignoreCase = true) || it.definition.contains(searchQuery, ignoreCase = true)) &&
                    (selectedDifficulty == "All" || it.difficulty.equals(selectedDifficulty, ignoreCase = true)) &&
                    (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true))
                }
            } else {
                repository.searchWords(
                    query = searchQuery,
                    difficulty = if (selectedDifficulty == "All") null else selectedDifficulty,
                    category = if (selectedCategory == "All") null else selectedCategory
                )
            }
        }
    }

    val diffScrollState = rememberScrollState()
    val catScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LexiDarkBg)
            .padding(top = 20.dp, start = 24.dp, end = 24.dp)
    ) {
        // HEADER WITH SCOPE TABS (ALL / SAVED)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore",
                color = LexiDarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Segmented switch for All vs Saved
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LexiDarkSurfaceSubtle)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedScopeTab == 0) LexiDarkBg else Color.Transparent)
                        .clickable { selectedScopeTab = 0 }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All",
                        color = if (selectedScopeTab == 0) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedScopeTab == 1) LexiDarkBg else Color.Transparent)
                        .clickable { selectedScopeTab = 1 }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Saved",
                        color = if (selectedScopeTab == 1) LexiDarkTextPrimary else LexiDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEARCH INPUT BAR
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
                        Text(
                            text = if (selectedScopeTab == 1) "Search saved words..." else "Search vocabulary...",
                            color = LexiDarkTextSecondary,
                            fontSize = 14.sp
                        )
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

        Spacer(modifier = Modifier.height(14.dp))

        // DIFFICULTY FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(diffScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            difficulties.forEach { diff ->
                LexiChip(
                    text = diff,
                    isSelected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = diff }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CATEGORY FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(catScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                LexiChip(
                    text = cat,
                    isSelected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SMART QUICK REVIEW BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LexiDarkSurfaceSubtle)
                .clickable {
                    reviewWords = repository.getReviewWords(5)
                    reviewIndex = 0
                    isReviewDialogOpen = true
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Smart Quick Review",
                        color = LexiDarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Practice saved & unmastered words",
                        color = LexiDarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = "Start →",
                color = LexiDarkAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LexiDivider(color = LexiDarkDivider)

        // WORD LIST ROWS
        if (wordList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedScopeTab == 1) "No saved words found" else "No words found",
                        color = LexiDarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedScopeTab == 1) "Bookmark words to build your personal lexicon" else "Try searching for another term",
                        color = LexiDarkTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(wordList, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWordClick(item) }
                            .padding(vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.word.replaceFirstChar { it.uppercase() },
                                color = LexiDarkTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Serif
                            )
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
                }
            }
        }
    }

    if (isReviewDialogOpen && reviewWords.isNotEmpty()) {
        val curWord = reviewWords.getOrNull(reviewIndex)
        if (curWord != null) {
            AlertDialog(
                onDismissRequest = { isReviewDialogOpen = false },
                containerColor = LexiDarkSurfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK REVIEW (${reviewIndex + 1}/${reviewWords.size})",
                            color = LexiDarkAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = curWord.difficulty.replaceFirstChar { it.uppercase() },
                            color = LexiDarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = curWord.word,
                            color = LexiDarkTextPrimary,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                        if (curWord.pronunciation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = curWord.pronunciation,
                                color = LexiDarkAccent,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = curWord.partOfSpeech.lowercase(),
                            color = LexiDarkTextSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = curWord.definition,
                            color = LexiDarkTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        if (curWord.exampleSentence.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "“${curWord.exampleSentence}”",
                                color = LexiDarkTextSecondary,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 18.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reviewIndex + 1 < reviewWords.size) {
                                reviewIndex++
                            } else {
                                isReviewDialogOpen = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LexiDarkAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (reviewIndex + 1 < reviewWords.size) "Next Word" else "Finish",
                            color = LexiDarkBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isReviewDialogOpen = false }) {
                        Text("Close", color = LexiDarkTextSecondary)
                    }
                }
            )
        }
    }
}
