package com.sarthak.lexilock

object WordRepository {

    val words = listOf(

        Word(
            word = "Tacenda",
            pronunciation = "/təˈsɛn.də/",
            partOfSpeech = "noun",
            meaning = "Things better left unsaid",
            example = "Some thoughts are better left tacenda.",
            level = "advanced"
        ),

        Word(
            word = "Eloquent",
            pronunciation = "/ˈel.ə.kwənt/",
            partOfSpeech = "adjective",
            meaning = "Fluent or persuasive in speaking or writing",
            example = "She gave an eloquent speech.",
            level = "intermediate"
        ),

        Word(
            word = "Curious",
            pronunciation = "/ˈkjʊr.i.əs/",
            partOfSpeech = "adjective",
            meaning = "Wanting to know or learn something",
            example = "He was curious about how the machine worked.",
            level = "beginner"
        ),

        Word(
            word = "Ephemeral",
            pronunciation = "/ɪˈfem.ər.əl/",
            partOfSpeech = "adjective",
            meaning = "Lasting for a very short time",
            example = "Internet trends can be ephemeral.",
            level = "advanced"
        )

    )
}