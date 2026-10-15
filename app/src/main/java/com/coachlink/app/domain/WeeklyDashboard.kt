package com.coachlink.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Input records deliberately have no Android/Room dependency, allowing isolated tests. */
data class FoodRecord(val day: LocalDate, val nutrition: MacroValues, val source: String = "manual")
data class ExerciseRecord(val day: LocalDate, val sets: Int, val reps: Int, val loadKg: Double)
data class WeightRecord(val day: LocalDate, val weightKg: Double)

data class WeeklyDay(
    val day: LocalDate,
    val foodCount: Int,
    val nutrition: MacroValues,
    val exerciseCount: Int,
) {
    val hasFoodData: Boolean get() = foodCount > 0
}

data class WeeklyDashboardData(
    val monday: LocalDate,
    val sunday: LocalDate,
    val days: List<WeeklyDay>,
    val recordedFoodDays: Int,
    val totalNutrition: MacroValues,
    /** Average only across days where food was actually logged; never treat absent days as zero intake. */
    val averageOnRecordedDays: MacroValues?,
    val exerciseDays: Int,
    /** Logs are exercise entries, not independent training sessions. */
    val exerciseLogCount: Int,
    /** External load only: bodyweight and unloaded exercises are not reflected. */
    val externalVolumeKg: Double,
    val photoEstimatedFoodEntries: Int,
    val weightsDuringWeek: List<WeightRecord>,
    /** Most recent measurement on or before this week's Sunday, can predate the week. */
    val latestKnownWeight: WeightRecord?,
    /** Change between two chronological measurements in this week, null otherwise. */
    val withinWeekWeightChangeKg: Double?,
)

private fun zero() = MacroValues(0.0, 0.0, 0.0, 0.0)
private fun add(a: MacroValues, b: MacroValues) = MacroValues(
    a.calories + b.calories, a.protein + b.protein, a.carbs + b.carbs, a.fat + b.fat,
)
private fun divide(a: MacroValues, days: Int) = MacroValues(
    a.calories / days, a.protein / days, a.carbs / days, a.fat / days,
)

fun firstDayOfWeek(reference: LocalDate): LocalDate =
    reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun summarizeWeek(
    reference: LocalDate,
    foods: List<FoodRecord>,
    exercises: List<ExerciseRecord>,
    weights: List<WeightRecord>,
): WeeklyDashboardData {
    val monday = firstDayOfWeek(reference)
    val sunday = monday.plusDays(6)
    val weekFoods = foods.filter { it.day >= monday && it.day <= sunday }
    val weekExercises = exercises.filter { it.day >= monday && it.day <= sunday }
    val weekWeights = weights.filter { it.day >= monday && it.day <= sunday }.sortedBy { it.day }
    val foodsByDay = weekFoods.groupBy { it.day }
    val exercisesByDay = weekExercises.groupBy { it.day }
    val days = (0L..6L).map { offset ->
        val date = monday.plusDays(offset)
        WeeklyDay(
            day = date,
            foodCount = foodsByDay[date].orEmpty().size,
            nutrition = foodsByDay[date].orEmpty().fold(zero()) { acc, food -> add(acc, food.nutrition) },
            exerciseCount = exercisesByDay[date].orEmpty().size,
        )
    }
    val total = days.fold(zero()) { acc, day -> add(acc, day.nutrition) }
    val recordedDays = days.count { it.hasFoodData }
    return WeeklyDashboardData(
        monday = monday,
        sunday = sunday,
        days = days,
        recordedFoodDays = recordedDays,
        totalNutrition = total,
        averageOnRecordedDays = if (recordedDays == 0) null else divide(total, recordedDays),
        exerciseDays = days.count { it.exerciseCount > 0 },
        exerciseLogCount = weekExercises.size,
        externalVolumeKg = weekExercises.sumOf { it.sets * it.reps * it.loadKg },
        photoEstimatedFoodEntries = weekFoods.count { it.source == "photo_ai_confirmed" },
        weightsDuringWeek = weekWeights,
        latestKnownWeight = weights.filter { it.day <= sunday }.maxByOrNull { it.day },
        withinWeekWeightChangeKg = if (weekWeights.size < 2) null
            else weekWeights.last().weightKg - weekWeights.first().weightKg,
    )
}
