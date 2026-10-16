package com.coachlink.one.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coachlink.one.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.asStateFlow
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
    fun addProgram(title:String, day:String) = run("Programme créé") {dao.addProgram(Program(title=title,dayLabel=day))}
    fun removeProgram(id:Long) = run("Programme supprimé") {dao.deleteProgram(id)}
    fun addExercise(x:Exercise) = run("Exercice ajouté") {dao.addExercise(x)}
    fun removeExercise(id:Long) = run("Exercice retiré") {dao.deleteExercise(id)}
    fun addLog(x:TrainingLog) = run("Séance enregistrée") {dao.addLog(x)}
    fun addFriend(x:FriendGoal) = run("Objectif ami importé") {dao.addFriendGoal(x)}
    fun removeFriend(id:Long) = run("Objectif ami retiré") {dao.deleteFriendGoal(id)}
}
