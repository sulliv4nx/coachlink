package com.coachlink.app.domain

import kotlin.math.roundToInt

data class MacroValues(val calories: Double, val protein: Double, val carbs: Double, val fat: Double)
data class MacroTarget(val calories: Int, val protein: Int, val carbs: Int, val fat: Int)

/** Values per 100 g, scaled to the exact quantity consumed. */
fun scaleFood(per100: MacroValues, grams: Double): MacroValues {
    require(grams > 0 && grams <= 5000 && grams.isFinite()) { "Quantité invalide" }
    require(listOf(per100.calories, per100.protein, per100.carbs, per100.fat).all { it.isFinite() && it >= 0 })
    val factor = grams / 100.0
    return MacroValues(per100.calories * factor, per100.protein * factor, per100.carbs * factor, per100.fat * factor)
}

/** Targets are chosen by the coach, not inferred from body weight alone. */
fun targetFromCalories(energyKcal: Int, weightKg: Double, proteinPerKg: Double, fatPerKg: Double): MacroTarget {
    require(energyKcal in 1000..6000) { "Calories hors plage de démonstration" }
    require(weightKg in 25.0..350.0 && proteinPerKg in 0.0..4.0 && fatPerKg in 0.0..3.0)
    val protein = (weightKg * proteinPerKg).roundToInt()
    val fat = (weightKg * fatPerKg).roundToInt()
    val remainderKcal = energyKcal - (4 * protein + 9 * fat)
    require(remainderKcal >= 0) { "Objectif impossible : calories insuffisantes" }
    return MacroTarget(energyKcal, protein, (remainderKcal / 4.0).roundToInt(), fat)
}

fun adherence(actual: Double, goal: Double): Float =
    if (goal <= 0) 0f else (actual / goal).toFloat().coerceIn(0f, 1f)
