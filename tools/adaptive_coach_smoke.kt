import com.coachlink.app.domain.*
import java.time.LocalDate

fun main() {
    val day = LocalDate.of(2026, 10, 15)
    val athlete = AthleteInputs(TrainingObjective.HYPERTROPHY, Muscle.CHEST, 4, 85.0,
        MacroTarget(2500, 160, 300, 70), 125.0, 5)
    val trainedChest = listOf(ActivityInput("Développé couché", day.minusDays(1), Muscle.CHEST, 5, 8, 9))
    val p1 = proposeSession(day, athlete, trainedChest)
    check(p1.focus != Muscle.CHEST) { "Chest should be skipped after heavy chest workout" }
    check(p1.exercises.none { it.muscle == Muscle.CHEST })
    val fresh = proposeSession(day, athlete, emptyList())
    check(fresh.focus == Muscle.CHEST) { "Untrained weak point should be prioritized" }
    val pain = proposeSession(day, athlete, listOf(ActivityInput("Squat", day.minusDays(2), Muscle.LEGS, 3, 10, 6, pain=true)))
    check(pain.exercises.none { it.muscle == Muscle.LEGS })
    check(pain.warnings.isNotEmpty())
    check(detectMuscle("Strict press") == Muscle.SHOULDERS)
    check(detectMuscle("Tractions") == Muscle.BACK)
    val noMeals = proposeSession(day, athlete.copy(recentNutritionDays=0, averageProteinG=null), emptyList())
    check(noMeals.explanation.any { it.contains("incomplet") })
    val menu = listOf(
        PlannedIngredient(1, "Déjeuner", "Riz blanc", 120.0, 2),
        PlannedIngredient(1, "Dîner", "riz blanc", 80.0, 1),
        PlannedIngredient(2, "Déjeuner", "Poulet", 200.0, 1)
    )
    val monday = LocalDate.of(2026, 10, 12)
    val week = buildGroceryList(menu, monday, monday.plusDays(6))
    check(week.items.size == 2)
    check(week.items.first { it.key == "riz blanc" }.grams == 320.0)
    check(week.items.first { it.key == "poulet" }.grams == 200.0)
    val month = buildGroceryList(menu, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
    val mondays = (1..31).count { LocalDate.of(2026,10,it).dayOfWeek.value == 1 }
    check(month.items.first { it.key == "riz blanc" }.grams == 320.0 * mondays)
    check(buildGroceryList(emptyList(), monday, monday.plusDays(6)).items.isEmpty())
    val dietary = reviewNutrition(day, MacroTarget(2500, 160, 300, 70),
        listOf(DailyNutrition(day, 1600.0, 90.0)), aiMode=true)
    check(dietary.avgCalories == null)
    check(dietary.advice.first().contains("1 jour"))
    val recorded = (0..3).map { DailyNutrition(day.minusDays(it.toLong()), 2500.0, 130.0) }
    val review = reviewNutrition(day, MacroTarget(2500, 160, 300, 70), recorded, true)
    check(review.avgCalories == 2500.0)
    check(review.daysRecorded == 4)
    println("PASS adaptive coach + shopping + nutrition (14 assertions)")
}
