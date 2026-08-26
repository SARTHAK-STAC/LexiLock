package com.sarthak.lexilock

object QuizRepository {

    val questions = listOf(

        QuizQuestion(
            question = "What does 'happy' mean?",
            options = listOf(
                "Feeling good",
                "Feeling angry",
                "Feeling tired",
                "Feeling afraid"
            ),
            correctAnswer = 0,
            difficulty = "beginner"
        ),

        QuizQuestion(
            question = "What does 'abundant' mean?",
            options = listOf(
                "Very rare",
                "Existing in large quantities",
                "Very expensive",
                "Difficult to find"
            ),
            correctAnswer = 1,
            difficulty = "intermediate"
        ),

        QuizQuestion(
            question = "What does 'ephemeral' mean?",
            options = listOf(
                "Extremely powerful",
                "Very complicated",
                "Lasting for a short time",
                "Extremely beautiful"
            ),
            correctAnswer = 2,
            difficulty = "advanced"
        ),

        QuizQuestion(
            question = "What does 'eloquent' mean?",
            options = listOf(
                "Fluent or persuasive in expression",
                "Unable to speak",
                "Very confused",
                "Extremely quiet"
            ),
            correctAnswer = 0,
            difficulty = "intermediate"
        ),

        QuizQuestion(
            question = "What does 'tacit' mean?",
            options = listOf(
                "Clearly written",
                "Understood without being directly expressed",
                "Very loud",
                "Completely incorrect"
            ),
            correctAnswer = 1,
            difficulty = "advanced"
        )
    )
}