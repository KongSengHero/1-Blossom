package com.ankilock.data
    
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
    
class ReadingHistoryChainTest { 
    @Test
    fun testGeneratedStoryChainDefaults() { 
        val story = GeneratedStory( 
            title = "Test Story", 
            content = "Once upon a time", 
            jlptLevel = "N4" 
        ) 
        assertEquals(story.id, story.chainId) 
        assertEquals(1, story.partNumber) 
        assertEquals(3, story.maxChainParts) 
        assertFalse(story.isCliffhanger) 
        assertFalse(story.isChainComplete) 
    } 
    
    @Test
    fun testCascadeDeletionFilterLogic() { 
        val chainId = "chain-123" 
        val part1 = GeneratedStory(id = "s1", chainId = chainId, partNumber = 1, title = "Part 1", content = "C1", jlptLevel = "N4") 
        val part2 = GeneratedStory(id = "s2", chainId = chainId, partNumber = 2, title = "Part 2", content = "C2", jlptLevel = "N4") 
        val part3 = GeneratedStory(id = "s3", chainId = chainId, partNumber = 3, title = "Part 3", content = "C3", jlptLevel = "N4") 
        val part4 = GeneratedStory(id = "s4", chainId = chainId, partNumber = 4, title = "Part 4", content = "C4", jlptLevel = "N4") 
        val other = GeneratedStory(id = "s5", chainId = "other", partNumber = 1, title = "Other", content = "C5", jlptLevel = "N4") 
        val stories = listOf(part1, part2, part3, part4, other) 
        
        val afterDeletingFromPart3 = stories.filterNot { it.chainId == chainId && it.partNumber >= 3 } 
        assertEquals(3, afterDeletingFromPart3.size) 
        assertTrue(afterDeletingFromPart3.any { it.id == "s1" }) 
        assertTrue(afterDeletingFromPart3.any { it.id == "s2" }) 
        assertTrue(afterDeletingFromPart3.any { it.id == "s5" }) 
        assertFalse(afterDeletingFromPart3.any { it.id == "s3" }) 
        assertFalse(afterDeletingFromPart3.any { it.id == "s4" }) 
        
        val afterDeletingWholeChain = stories.filterNot { it.chainId == chainId } 
        assertEquals(1, afterDeletingWholeChain.size) 
        assertEquals("s5", afterDeletingWholeChain[0].id) 
    } 
} 
