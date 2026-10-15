package com.coachlink.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coachlink.app.data.*
import com.coachlink.app.domain.MacroTarget
import com.coachlink.app.domain.MacroValues
import com.coachlink.app.domain.Muscle
import com.coachlink.app.domain.TrainingObjective
import com.coachlink.app.domain.CoachingMode
import com.coachlink.app.domain.SessionProposal
import com.coachlink.app.domain.PlannedIngredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class CoachViewModel(app: Application): AndroidViewModel(app) {
    private val repo = CoachRepository(CoachDatabase.get(app).dao())
    private val off = OpenFoodFactsRepository()
    private val photoClient = PhotoMealAnalysisClient()
    private val aiCoach = AiCoachingClient()
    val coachOnlineConfigured = aiCoach.configured
    private val _aiAdvice = MutableStateFlow<AiCoachAdvice?>(null)
    val aiAdvice = _aiAdvice.asStateFlow()
    private val _aiBusy = MutableStateFlow(false)
    val aiBusy = _aiBusy.asStateFlow()
    fun clearAiAdvice() { _aiAdvice.value = null }
    fun requestAiReview(now: LocalDate, athlete: com.coachlink.app.domain.AthleteInputs,
                        history: List<com.coachlink.app.domain.ActivityInput>) {
        if (_aiBusy.value) return
        _aiAdvice.value = null
        viewModelScope.launch {
            _aiBusy.value = true
            try {
                aiCoach.review(now, athlete, history)
                    .onSuccess { _aiAdvice.value = it }
                    .onFailure { _notice.value = it.message ?: "Analyse indisponible" }
            } finally { _aiBusy.value = false }
        }
    }
    val photoConfigured = photoClient.isConfigured
    private val _photoEstimate = MutableStateFlow<PhotoMealEstimate?>(null)
    val photoEstimate = _photoEstimate.asStateFlow()
    private val _photoLoading = MutableStateFlow(false)
    val photoLoading = _photoLoading.asStateFlow()
    fun clearPhotoEstimate() { _photoEstimate.value = null }
    fun analyzeMealPhoto(uri: Uri) {
        if (_photoLoading.value) return
        _photoEstimate.value = null
        viewModelScope.launch {
            _photoLoading.value = true
            try {
                photoClient.analyze(getApplication<Application>().contentResolver, uri)
                    .onSuccess { _photoEstimate.value = it }
                    .onFailure { _notice.value = it.message ?: "Photo non analysée" }
            } finally { _photoLoading.value = false }
        }
    }
    fun savePhotoFoods(foods: List<PhotoFood>, mealType: String): Boolean {
        if (foods.isEmpty() || foods.size > 20 || foods.any { f ->
            f.name.isBlank() || !f.grams.isFinite() || f.grams !in 0.1..5000.0 ||
                listOf(f.per100.calories, f.per100.protein, f.per100.carbs, f.per100.fat)
                    .any { !it.isFinite() || it < 0.0 }
        }) { inform("Vérifiez les quantités et les valeurs de chaque aliment"); return false }
        val day = LocalDate.now().toString()
        perform("Repas estimé enregistré après validation") {
            repo.addMeals(foods.map { f ->
                Meal(day = day, mealType = mealType, name = f.name,
                    grams = f.grams, calories100 = f.per100.calories,
                    protein100 = f.per100.protein, carbs100 = f.per100.carbs,
                    fat100 = f.per100.fat, source = "photo_ai_confirmed")
            })
        }
        _photoEstimate.value = null
        return true
    }
    val goal = repo.goal
    val meals = repo.meals
    val programs = repo.programs
    val exercises = repo.exercises
    val logs = repo.logs
    val metrics = repo.metrics
    val settings = repo.settings
    val extraWorkouts = repo.extraWorkouts
    val mealPlan = repo.mealPlan
    val crossfitSessions = repo.crossfitSessions
    private val _food = MutableStateFlow<FoodLookup?>(null)
    val food = _food.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    fun dismissNotice() { _notice.value = null }
    fun clearFood() { _food.value = null }
    fun inform(text: String) { _notice.value = text }
    private fun perform(doneMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onSuccess { _notice.value = doneMessage }
                .onFailure { _notice.value = it.message ?: "Erreur d'enregistrement" }
        }
    }
    fun lookup(code: String) {
        viewModelScope.launch {
            _loading.value = true
            _food.value = null
            try {
                off.lookup(code).onSuccess { _food.value = it }
                    .onFailure { _notice.value = it.message ?: "Produit introuvable" }
            } finally { _loading.value = false }
        }
    }
    fun saveGoal(target: MacroTarget) {
        if (target.calories !in 1000..6000 || minOf(target.protein, target.carbs, target.fat) < 0) {
            inform("Objectifs invalides"); return
        }
        perform("Objectifs modifiés") { repo.saveGoal(target) }
    }
    fun addMeal(name: String, barcode: String?, type: String, grams: Double, n: MacroValues): Boolean {
        if (name.isBlank() || grams !in 0.1..5000.0 || !grams.isFinite() ||
            listOf(n.calories, n.protein, n.carbs, n.fat).any { !it.isFinite() || it < 0 }) {
            inform("Vérifiez le nom, les quantités et les valeurs nutritionnelles"); return false
        }
        perform("Repas enregistré") {
            repo.addMeal(Meal(day = LocalDate.now().toString(), mealType = type,
                name = name.trim(), barcode = barcode?.ifBlank { null }, grams = grams,
                calories100 = n.calories, protein100 = n.protein, carbs100 = n.carbs, fat100 = n.fat))
        }
        return true
    }
    fun deleteMeal(id: Long) = perform("Aliment retiré") { repo.deleteMeal(id) }
    fun addProgram(title: String, day: String): Boolean {
        if (title.isBlank() || day.isBlank()) { inform("Renseignez le nom et le jour du programme"); return false }
        perform("Programme créé") { repo.createProgram(title.trim(), day.trim()) }
        return true
    }
    fun addExercise(programId: Long, name: String, sets: Int, reps: Int, load: Double, rest: Int, muscle: Muscle): Boolean {
        if (name.isBlank() || sets !in 1..30 || reps !in 1..100 || load !in 0.0..1000.0 || rest !in 0..1800 || !load.isFinite()) {
            inform("Valeurs d'exercice incorrectes"); return false
        }
        perform("Exercice ajouté") { repo.addExercise(Exercise(programId = programId, name = name.trim(), sets = sets, reps = reps, targetLoadKg = load, restSeconds = rest, muscleGroup = muscle.name)) }
        return true
    }
    fun deleteProgram(id: Long) = perform("Programme supprimé") { repo.deleteProgram(id) }
    fun addLog(exerciseId: Long, sets: Int, reps: Int, load: Double, effort: Int): Boolean {
        if (sets !in 1..30 || reps !in 1..100 || !load.isFinite() || load !in 0.0..1000.0 || effort !in 1..10) {
            inform("Séries, répétitions, charge ou RPE non valides"); return false
        }
        perform("Performance enregistrée") { repo.log(WorkoutLog(exerciseId = exerciseId, day = LocalDate.now().toString(), setsDone = sets, repsDone = reps, loadKg = load, perceivedEffort = effort)) }
        return true
    }
    fun addWeight(weight: Double): Boolean {
        if (!weight.isFinite() || weight !in 25.0..350.0) { inform("Poids invalide"); return false }
        perform("Pesée enregistrée") { repo.weigh(BodyMetric(day = LocalDate.now().toString(), weightKg = weight)) }
        return true
    }
    fun saveSettings(sport: CoachingMode, nutrition: CoachingMode, objective: TrainingObjective,
                     weak: Muscle?, sessions: Int): Boolean {
        if (sessions !in 1..7) { inform("Nombre de séances incorrect"); return false }
        perform("Profil d'accompagnement enregistré") {
            repo.saveSettings(AthleteSettings(sportMode = sport.name, nutritionMode = nutrition.name,
                objective = objective.name, weakMuscle = weak?.name ?: "NONE", sessionsPerWeek = sessions))
        }
        return true
    }

    fun logExtra(name: String, day: String, muscle: Muscle, sets: Int, reps: Int, rpe: Int, pain: Boolean): Boolean {
        if (name.trim().length !in 2..100 || sets !in 1..30 || reps !in 1..100 || rpe !in 1..10 ||
            runCatching { LocalDate.parse(day) }.isFailure || LocalDate.parse(day) > LocalDate.now()) {
            inform("Exercice, date, séries, répétitions ou effort incorrect"); return false
        }
        perform("Activité hors programme ajoutée : la prochaine séance sera recalculée") {
            repo.addExtraWorkout(ExtraWorkout(day = day, name = name.trim(), muscleGroup = muscle.name,
                sets = sets, reps = reps, rpe = rpe, pain = pain))
        }
        return true
    }
    fun saveCrossfit(day: String, title: String, format: String, wod: String,
                     minutes: Int, rpe: Int, muscles: Set<Muscle>, pain: Boolean): Boolean {
        if (title.trim().length !in 2..100 || wod.trim().length !in 3..3000 ||
            minutes !in 1..240 || rpe !in 1..10 || muscles.isEmpty() ||
            muscles.size > 7 || runCatching { LocalDate.parse(day) }.isFailure ||
            LocalDate.parse(day) > LocalDate.now()) {
            inform("Vérifiez la date, le WOD, la durée, l'effort et les muscles sollicités")
            return false
        }
        perform("Cours CrossFit enregistré : le coach prend ce WOD en compte") {
            repo.addCrossfitSession(CrossFitSession(day = day, title = title.trim(),
                format = format, wod = wod.trim(), durationMinutes = minutes,
                rpe = rpe, muscleGroups = muscles.joinToString(",") { it.name }, pain = pain))
        }
        return true
    }
    fun deleteCrossfit(id: Long) = perform("Cours CrossFit retiré") { repo.deleteCrossfitSession(id) }

    fun removeExtra(id: Long) = perform("Activité supprimée") { repo.deleteExtraWorkout(id) }
    fun acceptProposal(proposal: SessionProposal): Boolean {
        if (proposal.exercises.isEmpty() || proposal.exercises.size > 10) { inform("Séance non applicable"); return false }
        perform("Séance adaptée ajoutée aux programmes (à valider avant effort)") {
            repo.createSuggestedSession(proposal)
        }
        return true
    }
    fun addPlannedIngredient(weekday: Int, mealType: String, name: String, grams: Double, servings: Int): Boolean {
        if (weekday !in 1..7 || name.isBlank() || name.length > 100 || !grams.isFinite() || grams !in 0.1..5000.0 || servings !in 1..20) {
            inform("Aliment / quantité / portions invalides"); return false
        }
        perform("Aliment ajouté au menu hebdomadaire") {
            repo.addPlanIngredient(MealPlanIngredient(weekday = weekday, mealType = mealType,
                food = name.trim(), grams = grams, servings = servings))
        }
        return true
    }
    fun removePlannedIngredient(id: Long) = perform("Aliment retiré du menu") { repo.deletePlanIngredient(id) }
    fun copyRecentMealsToPlan(meals: List<Meal>): Boolean {
        val today = LocalDate.now()
        val week = meals.filter { meal -> runCatching {
            val day = LocalDate.parse(meal.day)
            !day.isAfter(today) && !day.isBefore(today.minusDays(6))
        }.getOrDefault(false) }
        if (week.isEmpty()) { inform("Aucun repas des 7 derniers jours à copier"); return false }
        val planned = week.map { meal -> MealPlanIngredient(
            weekday = LocalDate.parse(meal.day).dayOfWeek.value, mealType = meal.mealType,
            food = meal.name, grams = meal.grams, servings = 1)
        }
        perform("Menu remplacé par vos aliments des 7 derniers jours : pensez à vérifier les portions") {
            repo.replaceMealPlan(planned)
        }
        return true
    }

}
