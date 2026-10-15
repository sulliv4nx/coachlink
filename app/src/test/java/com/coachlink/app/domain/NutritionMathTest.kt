package com.coachlink.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NutritionMathTest {
    @Test fun scalesPortions() {
        val v = scaleFood(MacroValues(200.0, 10.0, 20.0, 5.0), 150.0)
        assertEquals(300.0, v.calories, 0.001)
        assertEquals(15.0, v.protein, 0.001)
        assertEquals(30.0, v.carbs, 0.001)
    }
    @Test fun macroTargetUsesEnergyBalance() {
        val t = targetFromCalories(2500, 80.0, 2.0, 1.0)
        assertEquals(160, t.protein)
        assertEquals(80, t.fat)
        assertEquals(285, t.carbs)
    }
    @Test fun impossibleMacroPlanRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            targetFromCalories(1000, 200.0, 4.0, 3.0)
        }
    }
}
