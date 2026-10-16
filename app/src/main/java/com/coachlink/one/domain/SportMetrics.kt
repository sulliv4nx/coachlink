package com.coachlink.one.domain

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

enum class SportGoalMetric(val label: String, val unit: String) {
    SESSIONS("Séances réalisées", "séances"),
    MINUTES("Temps d'entraînement", "min"),
    KCAL("Calories de sport enregistrées", "kcal")
}
enum class SportGoalPeriod(val label: String) { WEEK("Par semaine"), MONTH("Par mois") }

/** Training energy estimates are approximations, not a substitute for measured energy expenditure. */
fun estimateActivityCalories(weightKg: Double, durationMinutes: Int, activity: String): Int? {
    if (weightKg !in 25.0..350.0 || durationMinutes !in 1..360) return null
    val met = when (activity) {
        "Musculation" -> 3.5
        "CrossFit / WOD" -> 6.0
        "Cardio" -> 6.0
        "Marche / mobilité" -> 2.8
        else -> return null
    }
    return (met * 3.5 * weightKg / 200.0 * durationMinutes).roundToInt()
}

fun goalPeriodStart(day: LocalDate, period: SportGoalPeriod): LocalDate = when (period) {
    SportGoalPeriod.WEEK -> day.with(java.time.DayOfWeek.MONDAY)
    SportGoalPeriod.MONTH -> day.with(TemporalAdjusters.firstDayOfMonth())
}

data class SportSessionRecord(val day: LocalDate, val durationMinutes: Int, val calories: Int)
fun progressForSportGoal(records: List<SportSessionRecord>, metric: SportGoalMetric,
                         period: SportGoalPeriod, now: LocalDate): Int {
    val start = goalPeriodStart(now, period)
    val relevant = records.filter { !it.day.isBefore(start) && !it.day.isAfter(now) }
    return when (metric) {
        SportGoalMetric.SESSIONS -> relevant.size
        SportGoalMetric.MINUTES -> relevant.sumOf { it.durationMinutes }
        SportGoalMetric.KCAL -> relevant.sumOf { it.calories }
    }
}
