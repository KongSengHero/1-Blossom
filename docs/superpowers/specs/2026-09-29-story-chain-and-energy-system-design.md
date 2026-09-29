# Story Chain, Cliffhanger & Dynamic Energy System Design

## 1. Overview & Goals
Currently, AI stories forged in the application end quickly within a single installment. This update enables episodic, continuing story chains (between 3 and 5 parts) with optional cliffhanger endings, a collapsible multi-part Stories History view, cascading chain deletion with two-step root confirmation, a dedicated purple "Continue Story Chain" button styled after the Background Studio aesthetic, and an overhauled dynamic energy system tied to the user's daily study streak with 2-hour passive recharge.

---

## 2. Architecture & Data Model

### 2.1 ReadingModels: GeneratedStory Enhancements
File: `application/app/src/main/java/com/ankilock/data/ReadingModels.kt`

`GeneratedStory` is updated with five new fields:
- `chainId: String = id`
  - Unique identifier for the entire narrative arc.
  - When Part 1 is generated, `chainId = id`.
  - Parts 2 through 5 inherit the exact `chainId` of Part 1.
- `partNumber: Int = 1`
  - 1-based index of this chapter in the chain (Part 1, 2, 3, etc.).
- `maxChainParts: Int = 3`
  - The maximum length of this specific storyline, chosen randomly between 3 and 5 when Part 1 is created.
  - Inherited by all parts in the chain.
- `isCliffhanger: Boolean = false`
  - Set to `true` if the AI ended the episode with a suspenseful plot hook.
- `isChainComplete: Boolean = false`
  - Set to `true` when the final chapter has been forged and concluded, or if the chain reached its cap.

#### Backward Compatibility
Older stories saved without these fields in JSON automatically default to:
- `chainId = id`
- `partNumber = 1`
- `maxChainParts = 1`
- `isCliffhanger = false`
- `isChainComplete = true`
No data migration script is needed; deserialization provides safe fallbacks.

### 2.2 JSON Serialization
File: `application/app/src/main/java/com/ankilock/data/ReadingHistoryManager.kt`
- Deserialization in `getStories()` reads `optString("chainId", id)`, `optInt("partNumber", 1)`, `optInt("maxChainParts", 1)`, `optBoolean("isCliffhanger", false)`, and `optBoolean("isChainComplete", true)`.
- Serialization writes `chainId`, `partNumber`, `maxChainParts`, `isCliffhanger`, and `isChainComplete`.

---

## 3. Dynamic Streak Energy System & Recharge Engine

### 3.1 PreferencesManager Updates
File: `application/app/src/main/java/com/ankilock/data/PreferencesManager.kt`

#### Streak-Scaled Maximum Cap
The maximum energy cap scales according to `dailyStreakCount`:
- `< 3 days streak`: Max Cap = **5**
- `3 to 6 days streak`: Max Cap = **6**
- `7 to 9 days streak`: Max Cap = **7**
- `10 to 13 days streak`: Max Cap = **8**
- `14+ days streak`: Max Cap = **9**

Function: `getMaxStoryEnergy(): Int` computes this threshold dynamically.

#### Passive Recharge (1 Energy / 2 Hours)
- Key `KEY_LAST_ENERGY_RECHARGE_TIMESTAMP`: stores epoch milliseconds of the last recharge check.
- Recharge calculation:
  - If `currentEnergy < maxCap`:
    - `val elapsed = currentTime - lastRechargeTimestamp`
    - `val unitsToAdd = (elapsed / TWO_HOURS_MILLIS).toInt()`
    - If `unitsToAdd > 0`:
      - `currentEnergy = minOf(maxCap, currentEnergy + unitsToAdd)`
      - `lastRechargeTimestamp += unitsToAdd * TWO_HOURS_MILLIS`
  - If `currentEnergy >= maxCap`:
    - `lastRechargeTimestamp = currentTime`
- Midnight Reset:
  - When the date changes, energy resets fully to `getMaxStoryEnergy()`.

#### Tiered Continuation Energy Costs
Function: `getStoryContinuationCost(partNumber: Int): Int`
- Part 1 (New Story Forge): **1 Energy**
- Part 2 (Second continuation): **1 Energy**
- Part 3 (Third part): **2 Energy**
- Part 4 (Fourth part): **2 Energy**
- Part 5 (Fifth part): **3 Energy**

Function: `consumeStoryEnergy(cost: Int): Boolean` verifies `currentEnergy >= cost`, deducts `cost`, updates timestamp, and returns true.

### 3.2 DailyEnergyInfoDialog Overhaul
File: `application/app/src/main/java/com/ankilock/MainActivity.kt`
The dialog is upgraded to show:
- Current available energy vs dynamic maximum: `$storyEnergy / $maxEnergy Available`.
- Explanation of passive recharge: "+1 Energy restored every 2 hours".
- Streak bonus breakdown table/chips:
  - Base: 5 ⚡
  - 3d Streak: 6 ⚡
  - 7d Streak: 7 ⚡
  - 10d Streak: 8 ⚡
  - 14d Streak: 9 ⚡
- Story chain continuation costs:
  - Part 1: 1 ⚡ | Part 2: 1 ⚡ | Part 3: 2 ⚡ | Part 4: 2 ⚡ | Part 5: 3 ⚡.

---

## 4. Stories History UI: Collapsible Chain & Cascade Deletion

File: `application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt`

### 4.1 Chain Grouping & Collapsible View
- Stories in the History bottom sheet are grouped by `chainId`.
- Standalone stories (`maxChainParts == 1` or only 1 part exists) render as standard cards.
- Multi-part chains render a composite card:
  - **Header**: Shows series title, latest part number, thumbnail, and badge (`Chain • X Parts ⚡`).
  - **Collapse/Expand Toggle**: Pill button displaying `Show Chain (X) ▼` or `Hide Chain ▲`.
  - **Expanded Content**: Sub-list displaying each chapter in order (`Part 1`, `Part 2`, ...):
    - Chapter status (`Cliffhanger ⚡` or `Passed` score badge).
    - Quick tap to open and read that specific chapter.
    - Delete button per chapter.

### 4.2 Two-Step Root Deletion Warning
- When user taps Delete on **Part 1 (Main Arc)** of a multi-part chain:
  - **Dialog 1**: *"Delete Entire Story Chain? This is the main story arc. Deleting it will delete all [N] connected chapters in this chain."*
    - Options: "Cancel", "Delete Entire Chain"
  - If user selects "Delete Entire Chain", **Dialog 2 (Final Warning)** appears:
    - *"Permanent Deletion: Are you completely sure? All [N] stories, vocabulary items, and audio files will be permanently erased. This action cannot be undone."*
    - Options: "Keep Story", "Permanently Delete All"
  - Confirmation triggers `historyManager.deleteStoryChain(chainId)` removing all parts and associated cache.

### 4.3 Forward Cascade Deletion for Sub-Parts
- When user taps Delete on an intermediate part `K` (`K > 1`):
  - Confirmation dialog: *"Delete from Part K onwards? Chapters K through [N] will be removed. Previous chapters will be preserved."*
  - Deletion removes all stories where `chainId == target.chainId && partNumber >= K`.
  - The previous part (`K - 1`) becomes the latest active part in the chain and its completion state resets so the user can continue afresh from that point.

---

## 5. Continuation Engine & Purple Tactile Button

### 5.1 Placement & Styling
File: `application/app/src/main/java/com/ankilock/ui/reading/ReadingScreen.kt`
- Located directly above the Forge Button row.
- **Visibility Condition**:
  - `currentStory != null`
  - `!currentStory.isChainComplete`
  - `currentStory.partNumber < currentStory.maxChainParts`
- **Design & Theme**:
  - Styled with the Background Studio purple palette:
    - Button face: `BlossomColors.VioletPurple` (`0xFF9678B6`)
    - Button bevel/lip: `BlossomColors.VioletPurpleLip` (`0xFF6D528A`)
    - Glow / container: `BlossomColors.WisteriaVioletContainer` (`0xFF291F35`)
  - Label:
    - If previous part is a cliffhanger: `⚡ Resolve Cliffhanger • Part ${part + 1}/${max} (${cost} ⚡)`
    - Standard: `Continue Story Chain • Part ${part + 1}/${max} (${cost} ⚡)`
- **Energy Check**:
  - If user energy is less than required continuation cost, tapping displays a gentle toast/notification: *"Need [cost] Energy to continue this story. Recharges in [time] or resets at midnight."*

### 5.2 Prompt Chaining & Cliffhanger Logic
File: `application/app/src/main/java/com/ankilock/reading/GeminiStoryService.kt`
- New method `continueStoryChain(...)`:
  - Passes previous chapter summary, established characters, previous target vocabulary, and the closing sentence / cliffhanger hook.
  - Prompts Gemini to continue the narrative seamlessly from that exact moment.
  - If `nextPartNumber < maxChainParts`:
    - Incorporates instruction: *"Around 50% probability to end on a dramatic cliffhanger or unresolved suspenseful hook that demands a sequel. Set JSON field 'isCliffhanger': true if ended on a hook, false otherwise."*
  - If `nextPartNumber == maxChainParts` (The Final Part):
    - Incorporates strict finale instruction: *"[FINAL CHAPTER]: Resolve all ongoing plot threads, deliver a satisfying definitive conclusion, and DO NOT end on a cliffhanger. Set 'isCliffhanger': false and 'isChainComplete': true."*
- Parsed result sets `chainId = previousStory.chainId`, `partNumber = previousStory.partNumber + 1`, and `maxChainParts = previousStory.maxChainParts`.
