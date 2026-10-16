package com.coachlink.one.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable fun SportScreen(vm:TrackerViewModel,programs:List<Program>,exercises:List<Exercise>,logs:List<TrainingLog>,
                      goals:List<SportGoal>,sessions:List<SportSession>,weightKg:Double?) {
    var newGoal by remember { mutableStateOf(false) }
    var newSession by remember { mutableStateOf(false) }
    var newProgram by remember{mutableStateOf(false)}
    var editProgram by remember{mutableStateOf<Program?>(null)}
    var logExercise by remember{mutableStateOf<Exercise?>(null)}
    var timerDuration by remember{mutableIntStateOf(0)}
    var timerRemaining by remember{mutableIntStateOf(0)}
    var timerRunning by remember{mutableStateOf(false)}
    LaunchedEffect(timerRemaining,timerRunning) {
        if(timerRunning && timerRemaining>0) {delay(1000);timerRemaining-=1}
        else if(timerRemaining==0) timerRunning=false
    }
    Text("Entraînement",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
    Text("Programmation, charges et temps de repos",color=MaterialTheme.colorScheme.onSurfaceVariant)
    SportGoalsPanel(vm,goals,sessions) { newGoal=true }
    SportSessionsPanel(vm,sessions,onNew={ newSession=true })
    Button(onClick={newProgram=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" Créer une programmation")}
    Panel("CHRONOMÈTRE DE REPOS") {
        Text("${"%02d".format(timerRemaining/60)}:${"%02d".format(timerRemaining%60)}",style=MaterialTheme.typography.displayMedium,
            color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(60,90,120).forEach { sec ->
                FilterChip(selected=timerDuration==sec,onClick={
                    timerDuration=sec;timerRemaining=sec;timerRunning=false
                },label={Text("${sec}s")})
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={timerRunning=!timerRunning},enabled=timerRemaining>0){Text(if(timerRunning)"Pause" else "Démarrer")}
            OutlinedButton(onClick={timerRemaining=timerDuration;timerRunning=timerDuration>0}){Text("Recommencer")}
        }
        Text("Le repos démarre automatiquement après l'enregistrement d'un exercice.",style=MaterialTheme.typography.bodySmall)
    }
    Panel("MES PROGRAMMES") {
        if(programs.isEmpty())Text("Crée ton premier programme : force, hypertrophie ou circuit CrossFit.")
        programs.forEach{p->
            val items=exercises.filter{it.programId==p.id}
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text("${p.title} • ${p.dayLabel}",fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.secondary)
            items.forEach{ex->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)) {
                        Text(ex.name,fontWeight=FontWeight.SemiBold)
                        Text("${ex.sets} × ${ex.reps} · ${fmt(ex.loadKg)} kg · repos ${ex.restSec}s",style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick={logExercise=ex}){Text("Noter")}
                    IconButton(onClick={vm.removeExercise(ex.id)}){Icon(Icons.Default.Close,"Retirer")}
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick={editProgram=p}){Text("+ Exercice")}
                TextButton(onClick={vm.removeProgram(p.id)}){Text("Supprimer programme")}
            }
        }
    }
    Panel("HISTORIQUE DES SÉANCES") {
        if(logs.isEmpty())Text("Aucun exercice réalisé pour le moment.")
        logs.take(15).forEach{log->
            Text("${log.date} • ${log.name}",fontWeight=FontWeight.Bold)
            Text("${log.setsDone} séries × ${log.repsDone} reps • ${fmt(log.loadKg)} kg",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(newGoal) SportGoalDialog(vm){newGoal=false}
    if(newSession) SportSessionDialog(vm,weightKg){newSession=false}
    if(newProgram) ProgramDialog(vm){newProgram=false}
    editProgram?.let{p->ExerciseDialog(vm,p){editProgram=null}}
    logExercise?.let{ex->LogDialog(vm,ex,onClose={logExercise=null},onSaved={
        timerDuration=ex.restSec;timerRemaining=ex.restSec;timerRunning=ex.restSec>0
    })}
}

@Composable private fun ProgramDialog(vm:TrackerViewModel,onClose:()->Unit) {
    var title by remember{mutableStateOf("")}
    var day by remember{mutableStateOf("Lundi")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Nouveau programme")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Nom : force, jambes, WOD...",title,{title=it})
            Field("Jour / cycle",day,{day=it})
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={if(title.trim().length<2 || day.isBlank())error="Nom et jour requis" else {vm.addProgram(title.trim(),day.trim());onClose()}}){Text("Créer")}},
        dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}
@Composable private fun ExerciseDialog(vm:TrackerViewModel,program:Program,onClose:()->Unit) {
    var name by remember{mutableStateOf("")}
    var sets by remember{mutableStateOf("3")}
    var reps by remember{mutableStateOf("10")}
    var load by remember{mutableStateOf("0")}
    var rest by remember{mutableStateOf("90")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Ajouter un exercice")},text={
        Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(program.title,fontWeight=FontWeight.Bold)
            Field("Mouvement",name,{name=it})
            Field("Séries",sets,{sets=it},true)
            Field("Répétitions",reps,{reps=it},true)
            Field("Charge cible (kg)",load,{load=it},true)
            Field("Repos entre séries (secondes)",rest,{rest=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val s=sets.toIntOrNull();val r=reps.toIntOrNull();val k=double(load);val t=rest.toIntOrNull()
        if(name.trim().length<2 || s==null || s !in 1..30 || r==null || r !in 1..300 || k==null || k !in 0.0..1000.0 || t==null || t !in 0..1800)error="Valeurs incorrectes"
        else {vm.addExercise(Exercise(programId=program.id,name=name.trim(),sets=s,reps=r,loadKg=k,restSec=t));onClose()}
    }){Text("Ajouter")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}
@Composable private fun LogDialog(vm:TrackerViewModel,ex:Exercise,onClose:()->Unit,onSaved:()->Unit) {
    var sets by remember{mutableStateOf(ex.sets.toString())}
    var reps by remember{mutableStateOf(ex.reps.toString())}
    var load by remember{mutableStateOf(fmt(ex.loadKg))}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Performance • ${ex.name}")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Field("Séries réalisées",sets,{sets=it},true)
            Field("Répétitions réalisées",reps,{reps=it},true)
            Field("Charge réelle (kg)",load,{load=it},true)
            Text("Repos programmé : ${ex.restSec} s",style=MaterialTheme.typography.bodySmall)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val s=sets.toIntOrNull();val r=reps.toIntOrNull();val k=double(load)
        if(s==null || s !in 1..30 || r==null || r !in 1..300 || k==null || k !in 0.0..1000.0)error="Vérifie les performances"
        else {vm.addLog(TrainingLog(date=today,exerciseId=ex.id,name=ex.name,setsDone=s,repsDone=r,loadKg=k));onSaved();onClose()}
    }){Text("Enregistrer et lancer repos")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun SportGoalsPanel(vm:TrackerViewModel,goals:List<SportGoal>,sessions:List<SportSession>,onNew:()->Unit) {
    val records = sessions.mapNotNull { x ->
        runCatching { SportSessionRecord(LocalDate.parse(x.date),x.durationMinutes,x.caloriesBurned) }.getOrNull()
    }
    Panel("MES OBJECTIFS SPORTIFS") {
        Text("Tu peux suivre plusieurs objectifs en parallèle : séances, durée et calories de sport.",style=MaterialTheme.typography.bodySmall)
        Button(onClick=onNew,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Flag,null);Text(" Ajouter un objectif") }
        if(goals.isEmpty()) Text("Aucun objectif sportif pour le moment.")
        goals.forEach { goal ->
            val metric=SportGoalMetric.entries.find{it.name==goal.metric} ?: SportGoalMetric.SESSIONS
            val period=SportGoalPeriod.entries.find{it.name==goal.period} ?: SportGoalPeriod.WEEK
            val current=progressForSportGoal(records,metric,period,LocalDate.now())
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(goal.title,fontWeight=FontWeight.Bold)
                    Text("${period.label} • $current / ${goal.target} ${metric.unit}",style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick={vm.removeSportGoal(goal.id)}) {Icon(Icons.Default.DeleteOutline,"Supprimer l'objectif")}
            }
            LinearProgressIndicator(progress={(current.toFloat()/goal.target).coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(9.dp))
            if(current>=goal.target) Text("Objectif atteint pour cette période !",color=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun SportSessionsPanel(vm:TrackerViewModel,sessions:List<SportSession>,onNew:()->Unit) {
    val weekStart=LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    val thisWeek=sessions.filter { runCatching {
        val d=LocalDate.parse(it.date);!d.isBefore(weekStart)&&!d.isAfter(LocalDate.now())
    }.getOrDefault(false) }
    Panel("DÉPENSES DES SÉANCES") {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Metric("Calories sport / semaine","${thisWeek.sumOf{it.caloriesBurned}} kcal",MaterialTheme.colorScheme.secondary,Modifier.weight(1f))
            Metric("Séances / semaine","${thisWeek.size}",MaterialTheme.colorScheme.primary,Modifier.weight(1f))
        }
        Button(onClick=onNew,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.Add,null);Text(" Terminer une séance / noter mes calories")}
        Text("Dépenses estimées ou relevées sur montre : elles restent séparées des calories consommées, sans soustraction automatique.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(sessions.isEmpty()) Text("Renseigne la durée et les calories à la fin de l'entraînement.")
        sessions.take(12).forEach { x ->
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${x.activity} • ${x.date}",fontWeight=FontWeight.Bold)
                    Text("${x.durationMinutes} min • ${x.caloriesBurned} kcal • ${if(x.calorieMethod=="ESTIMATE") "Estimation" else "Saisie / montre"}",style=MaterialTheme.typography.bodySmall)
                    if(x.notes.isNotBlank()) Text(x.notes,style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick={vm.removeSportSession(x.id)}) { Icon(Icons.Default.DeleteOutline,"Supprimer") }
            }
        }
    }
}

@Composable private fun SportGoalDialog(vm:TrackerViewModel,onClose:()->Unit) {
    var title by remember{mutableStateOf("")}
    var metric by remember{mutableStateOf(SportGoalMetric.SESSIONS)}
    var period by remember{mutableStateOf(SportGoalPeriod.WEEK)}
    var target by remember{mutableStateOf("4")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Ajouter un objectif sportif")},text={
        Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Field("Nom (ex. 4 WOD par semaine)",title,{title=it})
            Text("Mesure",fontWeight=FontWeight.Bold)
            SportGoalMetric.entries.forEach { option ->
                FilterChip(selected=metric==option,onClick={metric=option},label={Text(option.label)})
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                SportGoalPeriod.entries.forEach { option ->
                    FilterChip(selected=period==option,onClick={period=option},label={Text(option.label)})
                }
            }
            Field("Cible (${metric.unit})",target,{target=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val t=target.toIntOrNull()
        if(title.trim().length !in 2..80 || t==null || t !in 1..200000) error="Nom ou valeur cible incorrects"
        else {vm.addSportGoal(SportGoal(title=title.trim(),metric=metric.name,period=period.name,target=t,createdAt=today));onClose()}
    }){Text("Ajouter")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun SportSessionDialog(vm:TrackerViewModel,weightKg:Double?,onClose:()->Unit) {
    var activity by remember{mutableStateOf("Musculation")}
    var date by remember{mutableStateOf(today)}
    var minutes by remember{mutableStateOf("60")}
    var calories by remember{mutableStateOf("")}
    var method by remember{mutableStateOf("MANUAL")}
    var notes by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    val types=listOf("Musculation","CrossFit / WOD","Cardio","Marche / mobilité")
    AlertDialog(onDismissRequest=onClose,title={Text("Bilan après séance")},text={
        Column(Modifier.heightIn(max=490.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Type de séance",fontWeight=FontWeight.Bold)
            types.forEach { item -> FilterChip(selected=activity==item,onClick={activity=item},label={Text(item)}) }
            Field("Date (AAAA-MM-JJ)",date,{date=it})
            Field("Durée (minutes)",minutes,{minutes=it},true)
            Field("Calories dépensées (montre / appareil)",calories,{calories=it;method="MANUAL"},true)
            OutlinedButton(onClick={
                val t=minutes.toIntOrNull()
                val estimate=if(t!=null) estimateActivityCalories(weightKg?:0.0,t,activity) else null
                if(estimate==null)error="Complète le profil (poids) et la durée" else {
                    calories=estimate.toString();method="ESTIMATE";error=""
                }
            },modifier=Modifier.fillMaxWidth()) {Text("Estimer à partir du poids et de la durée")}
            Text("Estimation très approximative (MET) : différente des calories actives de certaines montres.",
                style=MaterialTheme.typography.bodySmall)
            Field("Notes de séance (facultatif)",notes,{notes=it})
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val time=minutes.toIntOrNull();val kcal=calories.toIntOrNull()
        val okDate=runCatching{val d=LocalDate.parse(date);!d.isAfter(LocalDate.now())}.getOrDefault(false)
        if(!okDate || time==null || time !in 1..360 || kcal==null || kcal !in 0..6000 || notes.length>300)
            error="Vérifie date, durée, calories (0 à 6000) et notes"
        else {vm.addSportSession(SportSession(date=date,activity=activity,durationMinutes=time,caloriesBurned=kcal,calorieMethod=method,notes=notes.trim()));onClose()}
    }){Text("Enregistrer la séance")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}
