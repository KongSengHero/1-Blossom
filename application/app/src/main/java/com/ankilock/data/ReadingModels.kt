package com.ankilock.data
    
import java.util.UUID

data class AnkiVocabularyItem( 
    val kanji: String, 
    val reading: String = "", 
    val meaning: String = "", 
    val isSuspended: Boolean = false, 
    val state: String = "review", 
    val id: Long = 0L 
) { 
    val displayWord: String 
        get() = kanji.ifBlank { reading } 
} 

data class ReadingVocabularySummary( 
    val studiedCount: Int = 0, 
    val suspendedCount: Int = 0, 
    val words: List<AnkiVocabularyItem> = emptyList() 
) { 
    val totalCount: Int get() = studiedCount + suspendedCount 
} 

fun selectPrioritizedVocabulary( 
    eligibleVocab: List<AnkiVocabularyItem>, 
    count: Int 
): List<AnkiVocabularyItem> { 
    if (eligibleVocab.isEmpty() || count == 0) return emptyList() 
    val tenPercent = (eligibleVocab.size * 0.10f).let { if (it < 1f) 1 else kotlin.math.round(it).toInt() } 
    val oldestPool = eligibleVocab.take(tenPercent) 
    val latestPool = eligibleVocab.takeLast(tenPercent) 
    val middlePool = if (eligibleVocab.size > tenPercent * 2) { 
        eligibleVocab.subList(tenPercent, eligibleVocab.size - tenPercent) 
    } else { 
        emptyList() 
    } 
    if (count == -1) { 
        return (latestPool.shuffled() + oldestPool.shuffled() + middlePool.shuffled()).distinctBy { it.displayWord } 
    } 
    if (eligibleVocab.size <= count) { 
        return eligibleVocab.shuffled() 
    } 
    val selected = mutableListOf<AnkiVocabularyItem>() 
    val latestQuota = (count + 1) / 2 
    val oldestQuota = count - latestQuota 
    val pickedLatest = latestPool.shuffled().take(latestQuota) 
    selected.addAll(pickedLatest) 
    val pickedOldest = oldestPool.filter { item -> selected.none { it.displayWord == item.displayWord } }.shuffled().take(oldestQuota) 
    selected.addAll(pickedOldest) 
    val remainingNeeded = count - selected.size 
    if (remainingNeeded > 0) { 
        val remainingPool = eligibleVocab.filter { item -> selected.none { it.displayWord == item.displayWord } }.shuffled() 
        selected.addAll(remainingPool.take(remainingNeeded)) 
    } 
    return selected.shuffled() 
} 

data class StoryQuizQuestion( 
    val id: Int, 
    val questionText: String, 
    val options: List<String>, 
    val correctOptionIndex: Int, 
    val explanation: String = "" 
) 

data class GeneratedStory( 
    val id: String = UUID.randomUUID().toString(), 
    val title: String, 
    val content: String, 
    val furiganaContent: String? = null, 
    val jlptLevel: String, 
    val createdAt: Long = System.currentTimeMillis(), 
    val targetWords: List<String> = emptyList(), 
    val targetWordsData: List<StoryWordItem> = emptyList(), 
    val storyWords: List<StoryWordItem> = emptyList(), 
    val questions: List<StoryQuizQuestion> = emptyList(), 
    val theme: String? = null, 
    val topic: String? = null, 
    val isPinned: Boolean = false, 
    val quizScore: Int? = null, 
    val isPassed: Boolean = false, 
    val chainId: String = id, 
    val partNumber: Int = 1, 
    val maxChainParts: Int = 3, 
    val isCliffhanger: Boolean = false, 
    val isChainComplete: Boolean = false 
) 

data class CardModel( 
    val cardId: Long = 0L, 
    val noteId: Long = 0L, 
    val deckId: Long = 0L, 
    val cardOrd: Int = 0, 
    val kanji: String = "", 
    val kana: String = "", 
    val furigana: String = "", 
    val romaji: String = "", 
    val meaning: String = "", 
    val example: String = "", 
    val exampleSentence: String = "", 
    val exampleFurigana: String = "", 
    val exampleTranslation: String = "", 
    val exampleFuriganaLine: String = "", 
    val exampleSentenceLine: String = "", 
    val newCount: Int = 0, 
    val learnCount: Int = 0, 
    val reviewCount: Int = 0, 
    val intervalDays: Int = 0, 
    val isDue: Boolean = true, 
    val cardType: Int = 0, 
    val imageFileName: String = "" 
) 

data class AnkiDeck( 
    val id: Long, 
    val name: String, 
    val dueCardCount: Int = 0, 
    val newCount: Int = 0, 
    val learnCount: Int = 0, 
    val reviewCount: Int = 0 
) 

enum class ReviewEase(val value: Int, val label: String) { 
    AGAIN(1, "Again"), 
    HARD(2, "Hard"), 
    GOOD(3, "Good"), 
    EASY(4, "Easy") 
} 
