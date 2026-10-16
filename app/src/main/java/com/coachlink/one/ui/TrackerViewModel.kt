package com.coachlink.one.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
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
    val workoutPlans=dao.workoutPlans()
    val workoutBlocks=dao.workoutBlocks()
    val workoutMovements=dao.workoutMovements()
    val workoutRuns=dao.workoutRuns()
    val workoutSetResults=dao.workoutSetResults()
    val workoutBlockResults=dao.workoutBlockResults()
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

    fun createPlannedWorkout(name:String,date:String):Boolean {
        if(name.trim().length !in 2..100 || runCatching{LocalDate.parse(date)}.isFailure) {
            _notice.value="Nom et date (AAAA-MM-JJ) requis";return false
        }
        run("Séance ajoutée au planning") {dao.insertWorkoutPlan(WorkoutPlan(title=name.trim(),date=date))}
        return true
    }
    fun copyPlannedWorkout(source:WorkoutPlan,date:String):Boolean {
        if(runCatching{LocalDate.parse(date)}.isFailure) { _notice.value="Date incorrecte";return false }
        run("Séance copiée : tu peux ajuster ses blocs") {dao.cloneWorkoutPlan(source,date)}
        return true
    }
    fun removePlannedWorkout(id:Long) = run("Séance supprimée du planning") {dao.removePlanWithBlocks(id)}
    fun addWorkoutBlock(planId:Long, position:Int, title:String,format:String,durationSec:Int,rounds:Int):Boolean {
        if(title.trim().length !in 2..100 || format !in listOf("STRENGTH","AMRAP","EMOM","FOR_TIME","TABATA","WARMUP") ||
            durationSec !in 0..14400 || rounds !in 1..50 || (format in listOf("AMRAP","EMOM") && durationSec<=0)) {
            _notice.value="Vérifie le type de bloc, la durée et le nom";return false
        }
        run("Bloc ajouté") {dao.insertWorkoutBlock(WorkoutBlock(planId=planId,position=position,title=title.trim(),format=format,durationSec=durationSec,rounds=rounds))}
        return true
    }
    fun removeWorkoutBlock(id:Long)=run("Bloc supprimé") {dao.removeBlockWithMovements(id)}
    fun addWorkoutMovement(blockId:Long,position:Int,name:String,sets:Int,reps:Int,loadKg:Double,restSec:Int):Boolean {
        if(name.trim().length !in 2..100 || sets !in 1..30 || reps !in 1..300 || !loadKg.isFinite() || loadKg !in 0.0..1000.0 || restSec !in 0..1800) {
            _notice.value="Exercice : vérifie séries, reps, charge et repos";return false
        }
        run("Mouvement ajouté") {dao.insertWorkoutMovement(WorkoutMovement(blockId=blockId,position=position,name=name.trim(),sets=sets,reps=reps,loadKg=loadKg,restSec=restSec))}
        return true
    }
    fun removeWorkoutMovement(id:Long)=run("Mouvement supprimé") {dao.deleteWorkoutMovement(id)}
    fun startLiveWorkout(planId:Long) {
        viewModelScope.launch { kotlin.runCatching {dao.startWorkout(planId,System.currentTimeMillis())}
            .onSuccess { _notice.value=if(it)"Séance démarrée" else "Termine d'abord la séance ouverte ou ajoute un bloc au planning" }
            .onFailure { _notice.value="Impossible de démarrer : "+(it.message?:"erreur") } }
    }
    fun toggleLivePause(runId:Long) {
        viewModelScope.launch {kotlin.runCatching {
            val current=dao.workoutRun(runId)?:return@runCatching
            val now=System.currentTimeMillis()
            if(current.status=="RUNNING") {
                dao.updateWorkoutRun(current.copy(status="PAUSED",pausedAtMs=now,
                    restRemainingSec=restSecondsLeft(current.restStartedAtMs,current.restRemainingSec,now),
                    restStartedAtMs=0L))
            } else if(current.status=="PAUSED") {
                dao.updateWorkoutRun(current.copy(status="RUNNING",pausedTotalMs=current.pausedTotalMs+(now-(current.pausedAtMs?:now)).coerceAtLeast(0L),
                    pausedAtMs=null,restStartedAtMs=if(current.restRemainingSec>0)now else 0L))
            }
        }.onFailure{_notice.value=it.message?:"Erreur du chrono"}}
    }
    fun recordLiveSet(runId:Long,step:WorkoutMovement,reps:Int,kg:Double,status:String="DONE") {
        if(reps !in 0..300 || !kg.isFinite() || kg !in 0.0..1000.0 || status !in listOf("DONE","SKIP")) {
            _notice.value="Valeurs de série incorrectes";return
        }
        viewModelScope.launch {kotlin.runCatching {
            val run=dao.workoutRun(runId)?:return@runCatching
            if(run.status!="RUNNING") { _notice.value="Reprends la séance pour enregistrer la série";return@runCatching }
            val existing=dao.recordedSets(run.id,step.id)
            val nextNumber=(existing.maxOfOrNull{it.setNumber}?:0)+1
            dao.insertWorkoutSetResult(WorkoutSetResult(runId=runId,movementId=step.id,movementName=step.name,
                setNumber=nextNumber,reps=if(status=="SKIP")0 else reps,loadKg=if(status=="SKIP")0.0 else kg,status=status))
            val now=System.currentTimeMillis()
            dao.updateWorkoutRun(run.copy(restStartedAtMs=if(status=="DONE" && step.restSec>0)now else 0L,
                restRemainingSec=if(status=="DONE")step.restSec else 0))
        }.onSuccess {_notice.value=if(status=="DONE")"Série enregistrée" else "Exercice passé"}
          .onFailure{_notice.value=it.message?:"Impossible d'enregistrer la série"}}
    }
    fun skipLiveRest(runId:Long) = run("Repos terminé") {
        dao.workoutRun(runId)?.let{if(it.status=="RUNNING")dao.updateWorkoutRun(it.copy(restStartedAtMs=0L,restRemainingSec=0))}
    }
    fun nextLiveBlock(runId:Long,block:WorkoutBlock,blockCount:Int,score:String) {
        if(score.length>200){_notice.value="Score trop long";return}
        viewModelScope.launch {kotlin.runCatching {
            val run=dao.workoutRun(runId)?:return@runCatching
            if(run.status!="RUNNING" || run.blockIndex>=blockCount) return@runCatching
            val now=System.currentTimeMillis()
            val seconds=blockSeconds(run.startedAtMs,run.blockStartedAtMs,run.pausedTotalMs,
                run.blockPausedBaselineMs,run.pausedAtMs,now)
            dao.insertWorkoutBlockResult(WorkoutBlockResult(runId=run.id,blockId=block.id,blockName=block.title,score=score.trim(),elapsedSec=seconds))
            dao.updateWorkoutRun(run.copy(blockIndex=run.blockIndex+1,blockStartedAtMs=now,
                blockPausedBaselineMs=run.pausedTotalMs,restStartedAtMs=0L,restRemainingSec=0))
        }.onSuccess{_notice.value="Bloc validé"}.onFailure{_notice.value=it.message?:"Erreur d'étape"}}
    }
    fun finishLiveWorkout(runId:Long,planTitle:String,calories:String,notes:String):Boolean {
        val kcal=if(calories.isBlank())0 else calories.toIntOrNull()
        if(kcal==null || kcal !in 0..6000 || notes.length>300) { _notice.value="Vérifie les calories et les notes";return false }
        viewModelScope.launch {kotlin.runCatching {
            val active=dao.workoutRun(runId)?:return@runCatching
            if(active.status !in listOf("RUNNING","PAUSED"))return@runCatching
            val now=System.currentTimeMillis()
            val sec=sessionSeconds(active.startedAtMs,active.pausedTotalMs,active.pausedAtMs,now)
            dao.completeWorkout(active.copy(status="DONE",endedAtMs=now,restStartedAtMs=0L,restRemainingSec=0),
                SportSession(date=java.time.Instant.ofEpochMilli(active.startedAtMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                    activity="Séance planifiée • "+planTitle,durationMinutes=((sec+59)/60).toInt().coerceAtLeast(1),
                    caloriesBurned=kcal,calorieMethod=if(calories.isBlank())"UNKNOWN" else "MANUAL",notes=notes.trim()))
        }.onSuccess{_notice.value="Séance terminée : performances conservées"}.onFailure{_notice.value=it.message?:"Enregistrement impossible"}}
        return true
    }

    fun addFriend(x:FriendGoal) = run("Objectif ami importé") {dao.addFriendGoal(x)}
    fun removeFriend(id:Long) = run("Objectif ami retiré") {dao.deleteFriendGoal(id)}
}
