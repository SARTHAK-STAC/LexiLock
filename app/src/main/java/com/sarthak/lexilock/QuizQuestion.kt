package com.sarthak.lexilock

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val difficulty: String
)