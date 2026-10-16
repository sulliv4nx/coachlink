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
import kotlinx.coroutines.delay

@Composable fun SportScreen(vm:TrackerViewModel,programs:List<Program>,exercises:List<Exercise>,logs:List<TrainingLog>) {
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
    Button(onClick={newProgram=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" Créer une programmation")}
    Panel("CHRONOMÈTRE DE REPOS") {
        Text("${"%02d".format(timerRemaining/60)}:${"%02d".format(timerRemaining%60)}",style=MaterialTheme.typography.displayMedium,
            color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
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
