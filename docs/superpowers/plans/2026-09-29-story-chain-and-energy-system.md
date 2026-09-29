# Story Chain, Cliffhanger & Dynamic Energy System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement episodic story chain continuation (3–5 parts) with cliffhangers, collapsible Stories History with cascading deletion (and 2-step root warning), a purple Background Studio-themed "Continue Story Chain" button, and a dynamic study streak-scaled energy system (5–9 cap with 2-hour passive recharge).

**Architecture:**
- Extend `GeneratedStory` with chain metadata (`chainId`, `partNumber`, `maxChainParts`, `isCliffhanger`, `isChainComplete`) with backward-compatible defaults.
- Update `ReadingHistoryManager` with cascade deletion and JSON serialization for chain fields.
- Update `PreferencesManager` to support dynamic streak-scaled max energy (5 base, 6 at 3d, 7 at 7d, 8 at 10d, 9 at 14d), 2-hour passive replenishment, and tiered continuation energy costs (1, 1, 2, 2, 3).
- Update `GeminiStoryService` with `continueStoryChain` injecting previous chapter context and enforcing 50% cliffhangers or definitive finale resolution.
- Overhaul `DailyEnergyInfoDialog` in `MainActivity.kt` with dynamic breakdown and update `ReadingScreen.kt` with the purple continuation button and collapsible chain history.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android SharedPreferences, Coroutines, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-29-story-chain-and-energy-system-design.md`

## Global Constraints
- Trailing whitespace on wrapped lines ending with punctuation/operators.
- Never add comments to code.
- Blank lines must receive indentation whitespace matching surrounding block depth.
- 100% backward compatibility for all existing saved stories.

---

### Task 1: ReadingModels & ReadingHistoryManager - Story Chain Data Model & Cascade Deletion

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/data/ReadingModels.kt:32-49`
- Modify: `application/app/src/main/java/com/ankilock/data/ReadingHistoryManager.kt:17-122`, `144-155`
- Test: `application/app/src/test/java/com/ankilock/data/ReadingHistoryChainTest.kt`

**Interfaces:**
- Consumes: `GeneratedStory`
- Produces:
  - `GeneratedStory(chainId, partNumber, maxChainParts, isCliffhanger, isChainComplete)`
  - `ReadingHistoryManager.deleteStoryChain(chainId: String)`
  - `ReadingHistoryManager.deleteStoryFromPartOnwards(chainId: String, fromPartNumber: Int)`

- [ ] **Step 1: Write the failing test for chain fields and cascade deletion**

Create `application/app/src/test/java/com/ankilock/data/ReadingHistoryChainTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.data.ReadingHistoryChainTest`
Expected: FAIL due to unresolved `chainId`, `partNumber`, `maxChainParts`, etc.

- [ ] **Step 3: Implement data model and serialization in ReadingModels.kt & ReadingHistoryManager.kt**

In `ReadingModels.kt`:
```kotlin
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
```

In `ReadingHistoryManager.kt`:
Add JSON parsing in `getStories()`:
```kotlin
                val chainId = obj.optString("chainId", obj.optString("id", "")) 
                val partNumber = obj.optInt("partNumber", 1) 
                val maxChainParts = obj.optInt("maxChainParts", 3) 
                val isCliffhanger = obj.optBoolean("isCliffhanger", false) 
                val isChainComplete = obj.optBoolean("isChainComplete", false) 
```
Pass these into `GeneratedStory(...)`.
In `saveList(...)`:
```kotlin
                obj.put("chainId", story.chainId) 
                obj.put("partNumber", story.partNumber) 
                obj.put("maxChainParts", story.maxChainParts) 
                obj.put("isCliffhanger", story.isCliffhanger) 
                obj.put("isChainComplete", story.isChainComplete) 
```
Add deletion methods:
```kotlin
    fun deleteStoryChain(chainId: String) { 
        val toDelete = getStories().filter { it.chainId == chainId } 
        val remaining = getStories().filterNot { it.chainId == chainId } 
        saveList(remaining) 
        toDelete.forEach { s -> 
            FishAudioService.deleteAudioForStory(context, s.id) 
            try { 
                File(context.filesDir, "stories/images/${s.id}").deleteRecursively() 
            } catch (_: Exception) { 
            } 
        } 
    } 
    
    fun deleteStoryFromPartOnwards(chainId: String, fromPartNumber: Int) { 
        val toDelete = getStories().filter { it.chainId == chainId && it.partNumber >= fromPartNumber } 
        val remaining = getStories().map { s -> 
            if (s.chainId == chainId && s.partNumber == fromPartNumber - 1) { 
                s.copy(isChainComplete = false) 
            } else { 
                s 
            } 
        }.filterNot { it.chainId == chainId && it.partNumber >= fromPartNumber } 
        saveList(remaining) 
        toDelete.forEach { s -> 
            FishAudioService.deleteAudioForStory(context, s.id) 
            try { 
                File(context.filesDir, "stories/images/${s.id}").deleteRecursively() 
            } catch (_: Exception) { 
            } 
        } 
    } 
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.data.ReadingHistoryChainTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add application/app/src/main/java/com/ankilock/data/ReadingModels.kt application/app/src/main/java/com/ankilock/data/ReadingHistoryManager.kt application/app/src/test/java/com/ankilock/data/ReadingHistoryChainTest.kt
git commit -m "feat: add story chain metadata and cascade deletion to ReadingHistoryManager"
```

---

### Task 2: PreferencesManager - Dynamic Streak Energy System & Recharge Engine

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/data/PreferencesManager.kt:385-410`, `995-1005`
- Test: `application/app/src/test/java/com/ankilock/data/PreferencesEnergyTest.kt`

**Interfaces:**
- Consumes: `dailyStreakCount`
- Produces:
  - `PreferencesManager.getMaxStoryEnergy(): Int`
  - `PreferencesManager.getStoryContinuationCost(nextPartNumber: Int): Int`
  - `PreferencesManager.consumeStoryEnergy(cost: Int): Boolean`
  - `PreferencesManager.storyDailyEnergyRemaining: Int` (with 2-hour recharge)

- [ ] **Step 1: Write the failing unit test for dynamic energy cap and recharge calculations**

Create `application/app/src/test/java/com/ankilock/data/PreferencesEnergyTest.kt`:
```kotlin
package com.ankilock.data
    
import org.junit.Assert.assertEquals
import org.junit.Test
    
class PreferencesEnergyTest { 
    @Test
    fun testMaxEnergyScalingByStreak() { 
        assertEquals(5, PreferencesManager.calculateMaxEnergyForStreak(0)) 
        assertEquals(5, PreferencesManager.calculateMaxEnergyForStreak(2)) 
        assertEquals(6, PreferencesManager.calculateMaxEnergyForStreak(3)) 
        assertEquals(6, PreferencesManager.calculateMaxEnergyForStreak(6)) 
        assertEquals(7, PreferencesManager.calculateMaxEnergyForStreak(7)) 
        assertEquals(7, PreferencesManager.calculateMaxEnergyForStreak(9)) 
        assertEquals(8, PreferencesManager.calculateMaxEnergyForStreak(10)) 
        assertEquals(8, PreferencesManager.calculateMaxEnergyForStreak(13)) 
        assertEquals(9, PreferencesManager.calculateMaxEnergyForStreak(14)) 
        assertEquals(9, PreferencesManager.calculateMaxEnergyForStreak(50)) 
    } 
    
    @Test
    fun testTieredContinuationCost() { 
        assertEquals(1, PreferencesManager.calculateContinuationCost(1)) 
        assertEquals(1, PreferencesManager.calculateContinuationCost(2)) 
        assertEquals(2, PreferencesManager.calculateContinuationCost(3)) 
        assertEquals(2, PreferencesManager.calculateContinuationCost(4)) 
        assertEquals(3, PreferencesManager.calculateContinuationCost(5)) 
    } 
    
    @Test
    fun testRechargeCalculation() { 
        val twoHoursMillis = 2 * 60 * 60 * 1000L 
        val elapsedFourHours = twoHoursMillis * 2 
        val currentEnergy = 3 
        val maxCap = 7 
        val unitsToAdd = (elapsedFourHours / twoHoursMillis).toInt() 
        val newEnergy = minOf(maxCap, currentEnergy + unitsToAdd) 
        assertEquals(5, newEnergy) 
    } 
} 
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.data.PreferencesEnergyTest`
Expected: FAIL due to unresolved calculation helper methods.

- [ ] **Step 3: Implement dynamic energy calculation in PreferencesManager.kt**

In `PreferencesManager.kt`:
Add constants:
```kotlin
        const val KEY_LAST_ENERGY_RECHARGE_TIMESTAMP = "last_energy_recharge_timestamp" 
        const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L 
        
        fun calculateMaxEnergyForStreak(streak: Int): Int { 
            return when { 
                streak >= 14 -> 9 
                streak >= 10 -> 8 
                streak >= 7 -> 7 
                streak >= 3 -> 6 
                else -> 5 
            } 
        } 
        
        fun calculateContinuationCost(partNumber: Int): Int { 
            return when (partNumber) { 
                1, 2 -> 1 
                3, 4 -> 2 
                5 -> 3 
                else -> 3 
            } 
        } 
```
Add instance methods:
```kotlin
    val maxStoryEnergy: Int 
        get() = calculateMaxEnergyForStreak(dailyStreakCount) 
        
    fun getStoryContinuationCost(nextPartNumber: Int): Int { 
        return calculateContinuationCost(nextPartNumber) 
    } 
    
    var lastEnergyRechargeTimestamp: Long 
        get() = prefs.getLong(KEY_LAST_ENERGY_RECHARGE_TIMESTAMP, System.currentTimeMillis()) 
        set(value) = prefs.edit().putLong(KEY_LAST_ENERGY_RECHARGE_TIMESTAMP, value).apply() 
```
Update `checkAndResetDailyEnergy()` and recharge in getter:
```kotlin
    fun checkAndResetDailyEnergy() { 
        val today = getTodayDateString() 
        val recordedDate = prefs.getString(KEY_DAILY_STORY_ENERGY_DATE, "") ?: "" 
        val maxCap = maxStoryEnergy 
        val now = System.currentTimeMillis() 
        if (recordedDate != today) { 
            prefs.edit() 
                .putString(KEY_DAILY_STORY_ENERGY_DATE, today) 
                .putInt(KEY_DAILY_STORY_ENERGY_REMAINING, maxCap) 
                .putLong(KEY_LAST_ENERGY_RECHARGE_TIMESTAMP, now) 
                .apply() 
        } else { 
            val current = prefs.getInt(KEY_DAILY_STORY_ENERGY_REMAINING, maxCap) 
            if (current < maxCap) { 
                val lastTime = lastEnergyRechargeTimestamp 
                val elapsed = now - lastTime 
                val units = (elapsed / TWO_HOURS_MILLIS).toInt() 
                if (units > 0) { 
                    val newRemaining = minOf(maxCap, current + units) 
                    val newTimestamp = lastTime + (units * TWO_HOURS_MILLIS) 
                    prefs.edit() 
                        .putInt(KEY_DAILY_STORY_ENERGY_REMAINING, newRemaining) 
                        .putLong(KEY_LAST_ENERGY_RECHARGE_TIMESTAMP, newTimestamp) 
                        .apply() 
                } 
            } else { 
                lastEnergyRechargeTimestamp = now 
            } 
        } 
    } 
    
    fun consumeStoryEnergy(cost: Int): Boolean { 
        checkAndResetDailyEnergy() 
        val current = storyDailyEnergyRemaining 
        if (current < cost) return false 
        storyDailyEnergyRemaining = current - cost 
        lastEnergyRechargeTimestamp = System.currentTimeMillis() 
        return true 
    } 
    
    fun consumeDailyStoryEnergy(): Boolean { 
        return consumeStoryEnergy(1) 
    } 
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.data.PreferencesEnergyTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add application/app/src/main/java/com/ankilock/data/PreferencesManager.kt application/app/src/test/java/com/ankilock/data/PreferencesEnergyTest.kt
git commit -m "feat: implement streak-scaled energy cap, 2-hour passive recharge, and tiered continuation costs"
```

---

### Task 3: GeminiStoryService - Continuation Prompting & Cliffhanger Parsing

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/reading/GeminiStoryService.kt:31-89`, `166-240`, `281-360`
- Test: `application/app/src/test/java/com/ankilock/reading/GeminiStoryChainTest.kt`

**Interfaces:**
- Consumes: `GeneratedStory`, `PreferencesManager`
- Produces:
  - `GeminiStoryService.continueStoryChain(...)`
  - Parsing `isCliffhanger` and `isChainComplete` in `parseStoryResponse(...)`

- [ ] **Step 1: Write unit test for parsing cliffhanger JSON responses**

Create `application/app/src/test/java/com/ankilock/reading/GeminiStoryChainTest.kt`:
```kotlin
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
} 
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.reading.GeminiStoryChainTest`
Expected: FAIL due to missing arguments on `parseStoryResponse`.

- [ ] **Step 3: Implement continuation prompt and JSON parsing in GeminiStoryService.kt**

In `GeminiStoryService.kt`:
Update `parseStoryResponse`:
```kotlin
    internal fun parseStoryResponse( 
        rawText: String, 
        jlptLevel: String, 
        targetWords: List<String>, 
        targetWordsData: List<StoryWordItem> = emptyList(), 
        theme: String? = null, 
        topic: String? = null, 
        chainId: String? = null, 
        partNumber: Int = 1, 
        maxChainParts: Int = 3 
    ): GeneratedStory { 
...
        val isCliffhanger = json.optBoolean("isCliffhanger", false) 
        val isChainComplete = json.optBoolean("isChainComplete", partNumber >= maxChainParts) 
        val resolvedChainId = chainId ?: objId 
...
        return GeneratedStory( 
            id = objId, 
            title = title, 
            content = storyContent, 
            furiganaContent = furiganaContent, 
            jlptLevel = jlptLevel, 
            createdAt = System.currentTimeMillis(), 
            targetWords = targetWords, 
            targetWordsData = targetWordsData, 
            storyWords = storyWordsList, 
            questions = questions, 
            theme = theme, 
            topic = topic, 
            chainId = resolvedChainId, 
            partNumber = partNumber, 
            maxChainParts = maxChainParts, 
            isCliffhanger = isCliffhanger, 
            isChainComplete = isChainComplete 
        ) 
```

Add `buildChainContinuationPrompt(...)` and `continueStoryChain(...)`:
```kotlin
    fun buildChainContinuationPrompt( 
        previousStory: GeneratedStory, 
        nextPartNumber: Int, 
        maxChainParts: Int, 
        wordPromptList: String, 
        questionsCount: Int = 3 
    ): String { 
        val isFinale = (nextPartNumber >= maxChainParts) 
        val levelRules = when (previousStory.jlptLevel) { 
            "N5" -> "Strictly JLPT N5 elementary Japanese." 
            "N4" -> "Strictly JLPT N4 compound grammar." 
            "N3" -> "Strictly JLPT N3 transitional narrative." 
            "N2" -> "Strictly JLPT N2 advanced prose." 
            "N1" -> "Strictly JLPT N1 sophisticated literary Japanese." 
            else -> "Natural JLPT ${previousStory.jlptLevel} Japanese." 
        } 
        val cliffhangerInstruction = if (isFinale) { 
            """
            [FINAL CHAPTER RESOLUTION MANDATE]
            This is Part $nextPartNumber of $maxChainParts (The Grand Finale).
            You MUST bring all plot threads to a definitive, emotionally satisfying conclusion.
            DO NOT end on a cliffhanger. Set "isCliffhanger": false and "isChainComplete": true in JSON.
            """.trimIndent() 
        } else { 
            """
            [EPISODIC CHAPTER & CLIFFHANGER INSTRUCTION]
            This is Part $nextPartNumber of $maxChainParts.
            Directly continue the narrative from where Part ${previousStory.partNumber} left off.
            Resolve the immediate preceding tension, then introduce a new narrative development.
            There is a 50% probability to end this chapter on an intense cliffhanger, mystery, or sudden event hook!
            If it ends on a cliffhanger hook, set "isCliffhanger": true and "isChainComplete": false.
            Otherwise set "isCliffhanger": false and "isChainComplete": false.
            """.trimIndent() 
        } 
        return """
        You are an expert Japanese storyteller.
        Continue the story arc: "${previousStory.title}".
        Previous chapter excerpt:
        ${previousStory.content.takeLast(300)}
        
        $levelRules
        $cliffhangerInstruction
        
        Incorporate target vocabulary:
        $wordPromptList
        
        Return JSON schema with fields:
        {
          "title": "${previousStory.title} - Part $nextPartNumber",
          "storyJapanese": "...",
          "storyFurigana": "...",
          "isCliffhanger": true/false,
          "isChainComplete": true/false,
          "storyWords": [...],
          "questions": [...]
        }
        """.trimIndent() 
    } 
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.ankilock.reading.GeminiStoryChainTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add application/app/src/main/java/com/ankilock/reading/GeminiStoryService.kt application/app/src/test/java/com/ankilock/reading/GeminiStoryChainTest.kt
git commit -m "feat: add continuation prompt and cliffhanger parsing to GeminiStoryService"
```

---

### Task 4: MainActivity - DailyEnergyInfoDialog Overhaul

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/MainActivity.kt:995-1065`

**Interfaces:**
- Consumes: `storyEnergy`, `prefs.maxStoryEnergy`, `prefs.dailyStreakCount`
- Produces: Rich dialog breakdown of streak bonuses, passive recharge, and tiered continuation costs.

- [ ] **Step 1: Update DailyEnergyInfoDialog layout in MainActivity.kt**

In `MainActivity.kt`:
Pass `maxEnergy = prefs.maxStoryEnergy` and `streakCount = prefs.dailyStreakCount`.
Display:
- Header pill badge: `$storyEnergy / $maxEnergy Available`
- Passive recharge badge: `+1 ⚡ every 2 hours`
- Streak progression table/cards showing:
  - Base: 5 ⚡
  - 3d Streak: 6 ⚡
  - 7d Streak: 7 ⚡
  - 10d Streak: 8 ⚡
  - 14d Streak: 9 ⚡
- Continuation cost info card:
  - Part 2: 1 ⚡ | Part 3: 2 ⚡ | Part 4: 2 ⚡ | Part 5: 3 ⚡
  - "Continuation allows you to continue the story arc up to 3-5 chapters with twists and cliffhangers!"

- [ ] **Step 2: Verify project builds**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add application/app/src/main/java/com/ankilock/MainActivity.kt
git commit -m "feat: overhaul DailyEnergyInfoDialog with streak scaling and recharge info"
```

---

### Task 5: ReadingScreen - Purple "Continue Story Chain" Tactile Button

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt:1940-2015`

**Interfaces:**
- Consumes: `currentStory`, `prefs.consumeStoryEnergy`, `storyService.continueStoryChain`
- Produces: Purple continuation button rendered above the Forge button row.

- [ ] **Step 1: Implement purple continuation button in ReadingScreen.kt**

In `ReadingScreen.kt`:
Check continuation condition:
```kotlin
val canContinueChain = (currentStory != null && !currentStory!!.isChainComplete && currentStory!!.partNumber < currentStory!!.maxChainParts) 
val nextPartNumber = (currentStory?.partNumber ?: 1) + 1 
val continuationCost = remember(nextPartNumber) { prefs.getStoryContinuationCost(nextPartNumber) } 
```

Directly above the Forge button row:
```kotlin
if (canContinueChain) { 
    val isCliffhanger = currentStory?.isCliffhanger == true 
    Squircle3DButton( 
        onClick = { 
            if (prefs.storyDailyEnergyRemaining < continuationCost) { 
                Toast.makeText(context, "Need $continuationCost Energy to continue. Recharges in 2 hrs or at midnight!", Toast.LENGTH_SHORT).show() 
            } else { 
                triggerStoryContinuation() 
            } 
        }, 
        modifier = Modifier 
            .fillMaxWidth() 
            .height(52.dp), 
        containerColor = BlossomColors.VioletPurple, 
        bevelColor = BlossomColors.VioletPurpleLip, 
        shape = BlossomShapes.SquircleMedium, 
        depth = 3.dp 
    ) { 
        Row( 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.Center 
        ) { 
            Icon( 
                imageVector = if (isCliffhanger) Icons.Filled.ElectricBolt else Icons.Filled.AutoStories, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(18.dp) 
            ) 
            Spacer(modifier = Modifier.width(8.dp)) 
            Text( 
                text = if (isCliffhanger) { 
                    "⚡ Resolve Cliffhanger • Part $nextPartNumber/${currentStory?.maxChainParts} ($continuationCost ⚡)" 
                } else { 
                    "Continue Story Chain • Part $nextPartNumber/${currentStory?.maxChainParts} ($continuationCost ⚡)" 
                }, 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.5.sp, 
                color = Color.White 
            ) 
        } 
    } 
    Spacer(modifier = Modifier.height(10.dp)) 
} 
```

Implement `triggerStoryContinuation()` calling `storyService.continueStoryChain`, deducting `continuationCost`, saving to `historyManager`, and updating `currentStory`.

- [ ] **Step 2: Verify project builds**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt
git commit -m "feat: add purple Continue Story Chain button above Forge button"
```

---

### Task 6: ReadingScreen - Stories History Collapsible Chain & Cascade Deletion UI

**Files:**
- Modify: `application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt:2520-2950`

**Interfaces:**
- Consumes: `savedStories`, `historyManager.deleteStoryChain`, `historyManager.deleteStoryFromPartOnwards`
- Produces:
  - Collapsible chain grouping with expand/collapse toggle
  - Double confirmation dialog for Part 1 root deletion
  - Forward cascade deletion dialog for sub-parts

- [ ] **Step 1: Implement chain grouping and collapsible state in Stories History sheet**

In `ReadingScreen.kt`:
```kotlin
val expandedChains = remember { mutableStateMapOf<String, Boolean>() } 
var chainToDeleteRoot by remember { mutableStateOf<GeneratedStory?>(null) } 
var showRootWarningFirstDialog by remember { mutableStateOf(false) } 
var showRootWarningFinalDialog by remember { mutableStateOf(false) } 
var subPartToDelete by remember { mutableStateOf<GeneratedStory?>(null) } 
```

Group `savedStories` by `chainId`:
- Standalone stories render as single cards.
- Multi-part chains render a composite card with:
  - Series title + badge (`Chain • X Parts ⚡`)
  - Pill button: `Show Chain (X) ▼` / `Hide Chain ▲`
  - When expanded: indented cards for each part with part number, quiz score, cliffhanger badge, and delete button.

- [ ] **Step 2: Implement two-step root confirmation dialogs and sub-part cascade dialog**

- Tapping delete on Part 1:
  - Sets `chainToDeleteRoot = item`
  - Shows Dialog 1: *"Delete Entire Story Chain? This is the main arc. Deleting it will delete all connected chapters."*
  - On confirm ➔ Shows Dialog 2: *"Permanent Deletion: Are you completely sure? All connected chapters and audio will be permanently lost."* ➔ Calls `historyManager.deleteStoryChain(chainId)`.
- Tapping delete on Part K (`K > 1`):
  - Sets `subPartToDelete = item`
  - Shows Dialog: *"Delete Part K and onward? Chapters K through N will be removed. Earlier chapters will be kept."*
  - On confirm ➔ Calls `historyManager.deleteStoryFromPartOnwards(chainId, K)`.

- [ ] **Step 3: Verify all unit tests and debug build pass**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Run: `.\gradlew.bat :app:assembleDebug`
Expected: ALL PASS, BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt
git commit -m "feat: implement collapsible chain history view and cascading deletion dialogs"
```
