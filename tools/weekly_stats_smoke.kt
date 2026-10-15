import com.coachlink.app.domain.*
import java.time.LocalDate

private fun checkApprox(actual: Double, expected: Double) {
    check(kotlin.math.abs(actual - expected) < 0.001) { "Expected $expected, got $actual" }
}
fun main() {
    val foods = listOf(
        FoodRecord(LocalDate.parse("2026-10-12"), MacroValues(750.0, 50.0, 75.0, 20.0)),
        FoodRecord(LocalDate.parse("2026-10-12"), MacroValues(250.0, 10.0, 25.0, 10.0), "photo_ai_confirmed"),
        FoodRecord(LocalDate.parse("2026-10-15"), MacroValues(2000.0, 100.0, 180.0, 80.0)),
        FoodRecord(LocalDate.parse("2026-10-05"), MacroValues(9999.0, 0.0, 0.0, 0.0)),
    )
    val logs = listOf(
        ExerciseRecord(LocalDate.parse("2026-10-12"), 3, 8, 40.0),
        ExerciseRecord(LocalDate.parse("2026-10-12"), 4, 6, 30.0),
        ExerciseRecord(LocalDate.parse("2026-10-15"), 3, 10, 0.0),
    )
    val weights = listOf(
        WeightRecord(LocalDate.parse("2026-10-12"), 105.5),
        WeightRecord(LocalDate.parse("2026-10-15"), 105.1),
    )
    val summary = summarizeWeek(LocalDate.parse("2026-10-18"), foods, logs, weights)
    check(summary.monday == LocalDate.parse("2026-10-12"))
    check(summary.sunday == LocalDate.parse("2026-10-18"))
    check(summary.recordedFoodDays == 2)
    checkApprox(summary.totalNutrition.calories, 3000.0)
    checkApprox(summary.averageOnRecordedDays!!.calories, 1500.0)
    check(summary.exerciseDays == 2 && summary.exerciseLogCount == 3)
    checkApprox(summary.externalVolumeKg, 1680.0)
    check(summary.photoEstimatedFoodEntries == 1)
    checkApprox(summary.withinWeekWeightChangeKg!!, -0.4)
    check(summarizeWeek(LocalDate.parse("2026-10-15"), emptyList(), emptyList(), emptyList()).averageOnRecordedDays == null)
    println("OK: weekly analytics: ISO week, logged-day average, exercises, external volume, photo records, body metrics")
}
