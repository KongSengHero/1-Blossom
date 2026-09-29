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
    
    @Test
    fun testStripRubyFuriganaFromTitleAndContent() { 
        val json = """
            {
              "title": "消[き]えた隣[となり]の人 (Part 2)",
              "storyJapanese": "彼[かれ]は静[しず]かに歩[ある]き出[だ]しました。",
              "storyFurigana": "彼[かれ]は静[しず]かに歩[ある]き出[だ]しました。",
              "isCliffhanger": true,
              "isChainComplete": false,
              "storyWords": [
                {
                  "kanji": "静か",
                  "reading": "しずか",
                  "meaning": "quiet"
                }
              ],
              "questions": []
            }
        """.trimIndent() 
        
        val service = GeminiStoryService() 
        val story = service.parseStoryResponse( 
            rawText = json, 
            jlptLevel = "N3", 
            targetWords = emptyList(), 
            targetWordsData = emptyList(), 
            chainId = "chain-100", 
            partNumber = 2, 
            maxChainParts = 3 
        ) 
        
        assertEquals("消えた隣の人 (Part 2)", story.title) 
        assertEquals("彼は静かに歩き出しました。", story.content) 
        assertEquals("彼[かれ]は静[しず]かに歩[ある]き出[だ]しました。", story.furiganaContent) 
        assertEquals(1, story.storyWords.size) 
        assertEquals("静か", story.storyWords[0].kanji) 
    } 
    
    @Test
    fun testAutoParagraphSplit() { 
        val service = GeminiStoryService() 
        val singleLongParagraph = "昔々ある所におじいさんとおばあさんが住んでいました。毎日山へ芝刈りに行きました。川へ洗濯に行きました。大きな桃が流れてきました。二人はとても驚きました。桃を切ると中から男の子が生まれました。" 
        val formatted = service.autoParagraphSplit(singleLongParagraph) 
        assertTrue(formatted.contains("\n\n")) 
    } 
} 
