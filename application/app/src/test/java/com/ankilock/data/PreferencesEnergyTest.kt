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
