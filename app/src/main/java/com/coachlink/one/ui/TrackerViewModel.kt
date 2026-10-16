package com.coachlink.one.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coachlink.one.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrackerViewModel(app: Application): AndroidViewModel(app) {
    private val dao = TrackerDb.get(app).dao()
    val profile = dao.profile()
    val measurements = dao.measurements()
    val foods = dao.foods()
    val programs = dao.programs()
    val exercises = dao.exercises()
    val logs = dao.logs()
    val friends = dao.friends()
    val sportGoals = dao.sportGoals()
    val sportSessions = dao.sportSessions()
    private val _suggestions = MutableStateFlow<List<FoodSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private val _foodSearchBusy = MutableStateFlow(false)
    val foodSearchBusy = _foodSearchBusy.asStateFlow()
    private var lastFoodSearchAt = 0L
    fun clearSuggestions() { _suggestions.value = emptyList() }
    fun searchFoods(query: String) {
        if (query.trim().length < 3 || _foodSearchBusy.value) return
        // OFF limits full text search to 10 requests/min/IP. Conservative on-device throttle.
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastFoodSearchAt < 7000L) {
            _notice.value = "Attends quelques secondes avant une nouvelle recherche"
            return
        }
        lastFoodSearchAt = now
        viewModelScope.launch {
            _foodSearchBusy.value = true
            _suggestions.value = emptyList()
            FoodCatalog.search(query).onSuccess { _suggestions.value = it }
                .onFailure { _notice.value = it.message ?: "Recherche indisponible" }
            _foodSearchBusy.value = false
        }
    }
    private val _notice = MutableStateFlow("")
    val notice = _notice.asStateFlow()
    fun clearNotice() { _notice.value = "" }
    private fun run(success: String, block: suspend () -> Unit) {
        viewModelScope.launch { kotlin.runCatching { block() }
            .onSuccess { _notice.value = success }
            .onFailure { _notice.value = "Erreur : " + (it.message ?: "enregistrement") } }
    }
    fun saveProfile(x:Profile) = run("Profil enregistré") {dao.saveProfile(x)}
    fun addMeasurement(x:Measurement) = run("Mensurations enregistrées") {dao.addMeasurement(x)}
    fun removeMeasurement(id:Long) = run("Mesure supprimée") {dao.deleteMeasurement(id)}
    fun addFood(x:Food) = run("Repas enregistré") {dao.addFood(x)}
    fun removeFood(id:Long) = run("Aliment retiré") {dao.deleteFood(id)}
    fun addSportGoal(x:SportGoal) = run("Objectif sportif enregistré") {dao.addSportGoal(x)}
    fun removeSportGoal(id:Long) = run("Objectif retiré") {dao.deleteSportGoal(id)}
    fun addSportSession(x:SportSession) = run("Séance et dépense enregistrées") {dao.addSportSession(x)}
    fun removeSportSession(id:Long) = run("Séance retirée") {dao.deleteSportSession(id)}
    fun addProgram(title:String, day:String) = run("Programme créé") {dao.addProgram(Program(title=title,dayLabel=day))}
    fun removeProgram(id:Long) = run("Programme supprimé") {dao.deleteProgram(id)}
    fun addExercise(x:Exercise) = run("Exercice ajouté") {dao.addExercise(x)}
    fun removeExercise(id:Long) = run("Exercice retiré") {dao.deleteExercise(id)}
    fun addLog(x:TrainingLog) = run("Séance enregistrée") {dao.addLog(x)}
    fun addFriend(x:FriendGoal) = run("Objectif ami importé") {dao.addFriendGoal(x)}
    fun removeFriend(id:Long) = run("Objectif ami retiré") {dao.deleteFriendGoal(id)}
}
