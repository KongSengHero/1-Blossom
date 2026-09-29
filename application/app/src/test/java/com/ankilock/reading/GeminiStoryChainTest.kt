package com.ankilock.reading
    
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
    
class GeminiStoryChainTest { 
    @Test
    fun testParseCliffhangerFromGemini() { 
        val json = """
            {
              "title": "雨の日の出会い (Part 2)",
              "storyJapanese": "突然、窓ガラスが割れました！",
              "storyFurigana": "突[とつ]然[ぜん]、窓[まど]ガラスが割[わ]れました！",
              "isCliffhanger": true,
              "isChainComplete": false,
              "questions": []
            }
        """.trimIndent() 
        
        val service = GeminiStoryService() 
        val story = service.parseStoryResponse( 
            rawText = json, 
            jlptLevel = "N4", 
            targetWords = emptyList(), 
            targetWordsData = emptyList(), 
            chainId = "chain-999", 
            partNumber = 2, 
            maxChainParts = 4 
        ) 
        
        assertEquals("chain-999", story.chainId) 
        assertEquals(2, story.partNumber) 
        assertEquals(4, story.maxChainParts) 
        assertTrue(story.isCliffhanger) 
        assertFalse(story.isChainComplete) 
    } 
    
    @Test
    fun testParseChainCompleteFromGemini() { 
        val json = """
            {
              "title": "雨の日の出会い (Part 4 - 完)",
              "storyJapanese": "二人は笑顔で手を振って別れました。",
              "isCliffhanger": false,
              "isChainComplete": true,
              "questions": []
            }
        """.trimIndent() 
        
        val service = GeminiStoryService() 
        val story = service.parseStoryResponse( 
            rawText = json, 
            jlptLevel = "N4", 
            targetWords = emptyList(), 
            targetWordsData = emptyList(), 
            chainId = "chain-999", 
            partNumber = 4, 
            maxChainParts = 4 
        ) 
        
        assertEquals("chain-999", story.chainId) 
        assertEquals(4, story.partNumber) 
        assertEquals(4, story.maxChainParts) 
        assertFalse(story.isCliffhanger) 
        assertTrue(story.isChainComplete) 
    } 
} 
