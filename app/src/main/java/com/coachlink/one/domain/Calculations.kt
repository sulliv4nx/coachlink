package com.coachlink.one.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class Needs(val bmr: Int, val maintenanceKcal: Int, val targetKcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

data class NutritionEntry(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(other: NutritionEntry) = NutritionEntry(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)
}
val zeroNutrition = NutritionEntry(0.0, 0.0, 0.0, 0.0)
fun bmi(weightKg: Double, heightCm: Double): Double? =
    if (weightKg in 25.0..350.0 && heightCm in 100.0..240.0) weightKg / (heightCm / 100.0 * heightCm / 100.0) else null

/** Mifflin-St Jeor: informational estimate, not a diagnosis or automatic prescription. */
fun estimateNeeds(weightKg: Double, heightCm: Double, age: Int, sex: String, activity: Double, objective: String): Needs? {
    if (weightKg !in 25.0..350.0 || heightCm !in 100.0..240.0 || age !in 18..100 ||
        sex !in listOf("M", "F") || activity !in 1.2..1.9) return null
    val base = 10 * weightKg + 6.25 * heightCm - 5 * age + (if (sex == "M") 5 else -161)
    val maintenance = (base * activity).roundToInt()
    val change = when (objective) { "LOSS" -> -300; "GAIN" -> 200; else -> 0 }
    val kcal = max(1200, maintenance + change)
    val protein = (weightKg * 1.6).roundToInt()
    val fat = (weightKg * 0.8).roundToInt()
    val carbs = max(0, ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt())
    return Needs(base.roundToInt(), maintenance, kcal, protein, carbs, fat)
}
fun countdown(date: String, now: LocalDate = LocalDate.now()): Long? =
    runCatching { ChronoUnit.DAYS.between(now, LocalDate.parse(date)) }.getOrNull()
fun progress(start: Double, current: Double, target: Double): Float {
    val total = target - start
    if (total == 0.0) return if (current == target) 1f else 0f
    return ((current - start) / total).toFloat().coerceIn(0f, 1f)
}
fun toNutrition(kcal100: Double, p100: Double, c100: Double, f100: Double, grams: Double) =
    NutritionEntry(kcal100 * grams / 100, p100 * grams / 100, c100 * grams / 100, f100 * grams / 100)
