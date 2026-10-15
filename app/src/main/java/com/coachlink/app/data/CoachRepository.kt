package com.coachlink.app.data

import com.coachlink.app.domain.MacroTarget
import kotlinx.coroutines.flow.Flow

class CoachRepository(private val dao: CoachDao) {
    val goal: Flow<Goal?> = dao.goal()
    val meals: Flow<List<Meal>> = dao.meals()
    val programs: Flow<List<Program>> = dao.programs()
    val exercises: Flow<List<Exercise>> = dao.exercises()
    val logs: Flow<List<WorkoutLog>> = dao.logs()
    val metrics: Flow<List<BodyMetric>> = dao.metrics()
    val settings: Flow<AthleteSettings?> = dao.settings()
    val extraWorkouts: Flow<List<ExtraWorkout>> = dao.extraWorkouts()
    val mealPlan: Flow<List<MealPlanIngredient>> = dao.mealPlan()
    val crossfitSessions: Flow<List<CrossFitSession>> = dao.crossfitSessions()

    suspend fun saveGoal(target: MacroTarget) = dao.saveGoal(Goal(calories = target.calories, protein = target.protein, carbs = target.carbs, fat = target.fat))
    suspend fun addMeal(meal: Meal) = dao.addMeal(meal)
    suspend fun addMeals(meals: List<Meal>) = dao.addMeals(meals)
    suspend fun deleteMeal(id: Long) = dao.deleteMeal(id)
    suspend fun createProgram(title: String, label: String) = dao.addProgram(Program(title = title, dayLabel = label))
    suspend fun addExercise(item: Exercise) = dao.addExercise(item)
    suspend fun deleteProgram(id: Long) = dao.deleteProgram(id)
    suspend fun log(item: WorkoutLog) = dao.addLog(item)
    suspend fun weigh(item: BodyMetric) = dao.addMetric(item)
    suspend fun saveSettings(item: AthleteSettings) = dao.saveSettings(item)
    suspend fun addExtraWorkout(item: ExtraWorkout) = dao.addExtraWorkout(item)
    suspend fun deleteExtraWorkout(id: Long) = dao.deleteExtraWorkout(id)
    suspend fun addPlanIngredient(item: MealPlanIngredient) = dao.addMealPlanIngredient(item)
    suspend fun deletePlanIngredient(id: Long) = dao.deleteMealPlanIngredient(id)
    suspend fun replaceMealPlan(items: List<MealPlanIngredient>) = dao.replaceMealPlan(items)
    suspend fun addCrossfitSession(item: CrossFitSession) = dao.addCrossfitSession(item)
    suspend fun deleteCrossfitSession(id: Long) = dao.deleteCrossfitSession(id)
    suspend fun createSuggestedSession(session: com.coachlink.app.domain.SessionProposal) =
        dao.createSuggestedProgram(
            Program(title = session.title + " • suggestion validée", dayLabel = "Adapté"),
            session.exercises.map { ex ->
                Exercise(programId = 0, name = ex.name, sets = ex.sets, reps = ex.reps,
                    targetLoadKg = 0.0, restSeconds = ex.restSec, muscleGroup = ex.muscle.name)
            }
        )
}
