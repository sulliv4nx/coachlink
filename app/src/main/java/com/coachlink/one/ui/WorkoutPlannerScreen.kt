package com.coachlink.one.ui

import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.max

private val formats=listOf("STRENGTH","WARMUP","AMRAP","EMOM","FOR_TIME","TABATA")
private fun formatLabel(f:String)=when(f){
    "STRENGTH"->"Force";"WARMUP"->"Échauffement";"FOR_TIME"->"For Time";else->f
}
private fun normalDate(text:String):Boolean=runCatching{LocalDate.parse(text)}.isSuccess

/** Athlete creates full sessions in advance, then opens a persisted, resumable live run. */
@Composable
fun WorkoutPlannerScreen(vm:TrackerViewModel, plans:List<WorkoutPlan>, blocks:List<WorkoutBlock>,
    moves:List<WorkoutMovement>,runs:List<WorkoutRun>,setResults:List<WorkoutSetResult>,
    blockResults:List<WorkoutBlockResult>) {
    val active = runs.firstOrNull { it.status=="RUNNING" || it.status=="PAUSED" }
    if(active!=null) {
        LiveWorkoutScreen(vm,active,plans.firstOrNull{it.id==active.planId},blocks,moves,setResults,blockResults)
        return
    }
    var newPlan by remember {mutableStateOf(false)}
    var selectedId by remember {mutableStateOf<Long?>(null)}
    val chosen=plans.firstOrNull{it.id==selectedId}
    Panel("PLANNING • SÉANCES PRÉPARÉES") {
        Text("Prépare les blocs et les charges avant ton entraînement ; démarre la séance quand tu es prêt.",
            style=MaterialTheme.typography.bodySmall)
        Button(onClick={newPlan=true},modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add,null);Text(" Créer une séance complète")
        }
        if(plans.isEmpty())Text("Aucune séance planifiée pour le moment.")
        plans.sortedWith(compareBy<WorkoutPlan>{it.date}.thenBy{it.id}).forEach { p ->
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.title,fontWeight=FontWeight.ExtraBold)
                    Text("${p.date}  ·  ${blocks.count{it.planId==p.id}} bloc(s)",style=MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick={selectedId=if(selectedId==p.id)null else p.id}) {
                    Text(if(chosen?.id==p.id)"Fermer" else "Préparer")
                }
            }
            if(chosen?.id==p.id) {
                FilledTonalButton(onClick={vm.startLiveWorkout(p.id)},enabled=blocks.any{it.planId==p.id},
                    modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow,null); Text(" Démarrer cette séance")
                }
                Text("Une seule séance peut être en cours. La progression est conservée si tu quittes l'écran.",
                    style=MaterialTheme.typography.bodySmall)
            }
        }
    }
    if(chosen!=null) PlanEditor(vm,chosen,blocks.filter{it.planId==chosen.id}.sortedWith(compareBy<WorkoutBlock>{it.position}.thenBy{it.id}),moves,onDismiss={selectedId=null})
    val completed=runs.filter{it.status=="DONE"}
    if(completed.isNotEmpty()) Panel("HISTORIQUE • SÉANCES SUIVIES") {
        completed.take(7).forEach { r ->
            val plan=plans.firstOrNull{it.id==r.planId}
            val performed=setResults.filter{it.runId==r.id && it.status=="DONE"}
            val blockCount=blockResults.count{it.runId==r.id}
            Text("${plan?.title?:"Séance"} · $blockCount bloc(s) · ${performed.size} série(s)",fontWeight=FontWeight.Bold)
            Text("Volume enregistré : ${fmt(performed.sumOf{it.reps*it.loadKg},0)} kg-reps",
                style=MaterialTheme.typography.bodySmall)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
    }
    if(newPlan) PlanNewDialog(vm){newPlan=false}
}

@Composable private fun PlanEditor(vm:TrackerViewModel,plan:WorkoutPlan,blocks:List<WorkoutBlock>,moves:List<WorkoutMovement>,onDismiss:()->Unit) {
    var addBlock by remember {mutableStateOf(false)}
    var addTo by remember {mutableStateOf<WorkoutBlock?>(null)}
    var copyDialog by remember {mutableStateOf(false)}
    var deletion by remember {mutableStateOf(false)}
    Panel("COMPOSITION • ${plan.title}") {
        Text("Prévu le ${plan.date}. Compose un échauffement, de la force et/ou un WOD. Les mouvements restent modifiables avant le départ.",style=MaterialTheme.typography.bodySmall)
        blocks.forEachIndexed {i,block->
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text("${i+1}. ${block.title} · ${formatLabel(block.format)}",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.secondary)
            Text(if(block.format=="TABATA")"${block.rounds} cycles de 20s/10s" else if(block.durationSec>0) "Durée / cap : ${clockText(block.durationSec.toLong())}" else "Chronomètre libre",style=MaterialTheme.typography.bodySmall)
            moves.filter{it.blockId==block.id}.sortedBy{it.position}.forEach{move->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)) {
                        Text("• ${move.name}",fontWeight=FontWeight.Medium)
                        Text("${move.sets} × ${move.reps} · ${fmt(move.loadKg,0)} kg · repos ${move.restSec}s${if(move.supersetGroup.isNotBlank())" · Superset "+move.supersetGroup else ""}",style=MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick={vm.removeWorkoutMovement(move.id)}){Icon(Icons.Default.Close,"Retirer le mouvement")}
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedButton(onClick={addTo=block}) {Text("+ Mouvement")}
                TextButton(onClick={vm.removeWorkoutBlock(block.id)}) {Text("Retirer le bloc")}
            }
        }
        Button(onClick={addBlock=true},modifier=Modifier.fillMaxWidth()) {Text("+ Ajouter un bloc")}
        OutlinedButton(onClick={copyDialog=true},modifier=Modifier.fillMaxWidth()) {Text("Dupliquer pour une autre date")}
        TextButton(onClick={deletion=true}){Text("Supprimer la séance préparée")}
        TextButton(onClick=onDismiss){Text("Refermer le programme")}
    }
    if(addBlock) BlockDialog(vm,plan.id,(blocks.maxOfOrNull{it.position}?:-1)+1){addBlock=false}
    addTo?.let{block->MovementDialog(vm,block,(moves.filter{it.blockId==block.id}.maxOfOrNull{it.position}?:-1)+1){addTo=null}}
    if(copyDialog) CopyWorkoutDialog(vm,plan){copyDialog=false}
    if(deletion) AlertDialog(onDismissRequest={deletion=false},title={Text("Supprimer le planning ?")},
        text={Text("Cette opération retire les blocs préparés, mais les performances des séances terminées restent enregistrées.")},
        confirmButton={Button(onClick={vm.removePlannedWorkout(plan.id);deletion=false;onDismiss()}){Text("Supprimer")}},
        dismissButton={TextButton(onClick={deletion=false}){Text("Annuler")}})
}

@Composable private fun PlanNewDialog(vm:TrackerViewModel,onClose:()->Unit) {
    var title by remember{mutableStateOf("")}
    var date by remember{mutableStateOf(today)}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Préparer une séance")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Field("Nom de la séance",title,{title=it})
            Field("Date (AAAA-MM-JJ)",date,{date=it})
            Text("Après création, ouvre Préparer pour ajouter tes blocs et exercices.",style=MaterialTheme.typography.bodySmall)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        if(title.trim().length<2 || !normalDate(date))error="Nom ou date invalide"
        else if(vm.createPlannedWorkout(title,date))onClose()
    }){Text("Créer")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun CopyWorkoutDialog(vm:TrackerViewModel,plan:WorkoutPlan,onClose:()->Unit) {
    var date by remember{mutableStateOf(runCatching{LocalDate.parse(plan.date).plusWeeks(1).toString()}.getOrDefault(today))}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Dupliquer ${plan.title}")},text={
        Column{Field("Nouvelle date",date,{date=it});if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}
    },confirmButton={Button(onClick={if(!normalDate(date))error="Date incorrecte" else if(vm.copyPlannedWorkout(plan,date))onClose()}){Text("Dupliquer")}},
        dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun BlockDialog(vm:TrackerViewModel,planId:Long,position:Int,onClose:()->Unit) {
    var title by remember{mutableStateOf("")}
    var format by remember{mutableStateOf("STRENGTH")}
    var minutes by remember{mutableStateOf("12")}
    var rounds by remember{mutableStateOf("8")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Ajouter un bloc")},text={
        Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Titre : Échauffement, Force, WOD...",title,{title=it})
            Text("Type de bloc",fontWeight=FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                formats.forEach{f->FilterChip(selected=format==f,onClick={format=f},label={Text(formatLabel(f))})}
            }
            if(format!="TABATA")Field("Durée ou time cap (minutes, 0 = libre)",minutes,{minutes=it},true)
            if(format=="TABATA"){
                Field("Cycles Tabata (20s effort / 10s repos)",rounds,{rounds=it},true)
                Text("Timer intégré : 20 secondes d'effort, 10 secondes de repos.",style=MaterialTheme.typography.bodySmall)
            }
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val mins=if(format=="TABATA")0 else minutes.toIntOrNull()
        val count=rounds.toIntOrNull()
        if(mins==null || mins !in 0..240 || count==null || count !in 1..50)error="Durée ou cycles incorrects"
        else if(vm.addWorkoutBlock(planId,position,title,format,mins*60,count))onClose()
    }){Text("Ajouter")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun MovementDialog(vm:TrackerViewModel,block:WorkoutBlock,position:Int,onClose:()->Unit) {
    var name by remember{mutableStateOf("")}
    var sets by remember{mutableStateOf("3")}
    var reps by remember{mutableStateOf("10")}
    var kg by remember{mutableStateOf("0")}
    var rest by remember{mutableStateOf("90")}
    var group by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Mouvement • ${block.title}")},text={
        Column(Modifier.heightIn(max=490.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Exercice / mouvement",name,{name=it})
            if(name.length in 2..32) {
                searchExercises(name,4).forEach { suggestion ->
                    TextButton(onClick={name=suggestion.name},modifier=Modifier.fillMaxWidth()) {
                        Text("↳ ${suggestion.name} · ${suggestion.group}")
                    }
                }
            } else {
                Text("Exemples : squat, traction, thruster, rower…",style=MaterialTheme.typography.bodySmall)
            }
            Text("Groupe superset (facultatif)",fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                listOf("" to "Aucun","A" to "A","B" to "B","C" to "C").forEach { (key,label) ->
                    FilterChip(selected=group==key,onClick={group=key},label={Text(label)})
                }
            }
            Field("Séries prévues",sets,{sets=it},true)
            Field("Répétitions",reps,{reps=it},true)
            Field("Charge cible (kg)",kg,{kg=it},true)
            Field("Repos entre séries (secondes)",rest,{rest=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val s=sets.toIntOrNull();val r=reps.toIntOrNull();val k=double(kg);val restSec=rest.toIntOrNull()
        if(s==null || r==null || k==null || restSec==null)error="Valeurs incorrectes"
        else if(vm.addWorkoutMovement(block.id,position,name,s,r,k,restSec,group))onClose()
    }){Text("Ajouter")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun LiveWorkoutScreen(vm:TrackerViewModel,run:WorkoutRun,plan:WorkoutPlan?,
    blocks:List<WorkoutBlock>,moves:List<WorkoutMovement>,results:List<WorkoutSetResult>,blockResults:List<WorkoutBlockResult>) {
    val currentBlocks=blocks.filter{it.planId==run.planId}.sortedWith(compareBy<WorkoutBlock>{it.position}.thenBy{it.id})
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(run.id) {while(true){now=System.currentTimeMillis();delay(1000)}}
    val elapsed=sessionSeconds(run.startedAtMs,run.pausedTotalMs,run.pausedAtMs,now)
    val running=run.status=="RUNNING"
    val currentBlock=currentBlocks.getOrNull(run.blockIndex)
    var addSetFor by remember{mutableStateOf<WorkoutMovement?>(null)}
    var skipFor by remember{mutableStateOf<WorkoutMovement?>(null)}
    var blockScore by remember(currentBlock?.id){mutableStateOf("")}
    var finish by remember{mutableStateOf(false)}
    var quit by remember{mutableStateOf(false)}
    Text("SÉANCE EN DIRECT",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
    Panel(plan?.title?:"Entraînement en cours") {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text(if(running)"En cours" else "En pause",color=if(running)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
            Text(clockText(elapsed),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
        }
        LinearProgressIndicator(progress={if(currentBlocks.isEmpty())0f else (run.blockIndex.toFloat()/currentBlocks.size).coerceIn(0f,1f)},
            modifier=Modifier.fillMaxWidth().height(10.dp))
        Text("${run.blockIndex.coerceAtMost(currentBlocks.size)} / ${currentBlocks.size} blocs réalisés")
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick={vm.toggleLivePause(run.id)}){
                Icon(if(running)Icons.Default.Pause else Icons.Default.PlayArrow,null)
                Text(if(running)" Pause" else " Reprendre")
            }
            TextButton(onClick={finish=true}){Text("Terminer")}
        }
        Text("Tu peux verrouiller le téléphone ou changer d'onglet : la progression est conservée.",style=MaterialTheme.typography.bodySmall)
    }
    if(currentBlock!=null) {
        val blockElapsed=blockSeconds(run.startedAtMs,run.blockStartedAtMs,run.pausedTotalMs,run.blockPausedBaselineMs,run.pausedAtMs,now)
        val timer=wodTimer(currentBlock.format,blockElapsed,currentBlock.durationSec,currentBlock.rounds)
        Panel("BLOC ${run.blockIndex+1} • ${currentBlock.title}") {
            Text(formatLabel(currentBlock.format),color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
            Text(clockText(timer.secondsShown),style=MaterialTheme.typography.displayLarge,
                fontWeight=FontWeight.Black,color=if(timer.overtime)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            if(currentBlock.format=="TABATA")Text("${timer.phase} · cycle ${timer.round.coerceAtMost(currentBlock.rounds)} / ${currentBlock.rounds}",fontWeight=FontWeight.Bold)
            else if(currentBlock.format=="EMOM")Text(timer.phase,fontWeight=FontWeight.Bold)
            if(timer.overtime)Text("Temps prévu atteint · termine ou passe au bloc suivant.",color=MaterialTheme.colorScheme.error)
            moves.filter{it.blockId==currentBlock.id}.sortedBy{it.position}.forEach { step ->
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                QuickSetRecorder(vm,run,step,results,running,onSkip={skipFor=step})
            }
            if(run.restRemainingSec>0) {
                val remaining=restSecondsLeft(run.restStartedAtMs,run.restRemainingSec,now)
                Text("REPOS · ${clockText(remaining.toLong())}",style=MaterialTheme.typography.headlineMedium,
                    color=MaterialTheme.colorScheme.tertiary,fontWeight=FontWeight.ExtraBold)
                TextButton(onClick={vm.skipLiveRest(run.id)},enabled=running){Text("Passer le repos")}
            }
            OutlinedTextField(value=blockScore,onValueChange={blockScore=it.take(200)},
                label={Text(if(currentBlock.format=="AMRAP")"Score / tours + reps" else "Score ou note (facultatif)")},
                modifier=Modifier.fillMaxWidth(),maxLines=2)
            Button(onClick={vm.nextLiveBlock(run.id,currentBlock,currentBlocks.size,blockScore)},
                enabled=running,modifier=Modifier.fillMaxWidth()) {
                Text(if(run.blockIndex==currentBlocks.lastIndex)"Valider le dernier bloc" else "Valider et passer au bloc suivant")
            }
        }
    } else Panel("BILAN À FINALISER") {
        Text("Tous les blocs ont été terminés. Enregistre les calories de ta montre si tu en as, puis clôture.")
        Button(onClick={finish=true},modifier=Modifier.fillMaxWidth()) {Text("Terminer et enregistrer la séance")}
    }
    addSetFor?.let{ step->SetInputDialog(vm,run.id,step){addSetFor=null} }
    skipFor?.let{step->AlertDialog(onDismissRequest={skipFor=null},title={Text("Passer ${step.name} ?")},
        text={Text("La série sera marquée comme non réalisée.")},confirmButton={Button(onClick={vm.recordLiveSet(run.id,step,0,0.0,"SKIP");skipFor=null}){Text("Passer")}},
        dismissButton={TextButton(onClick={skipFor=null}){Text("Annuler")}})}
    if(finish) FinishWorkoutDialog(vm,run.id,plan?.title?:"Entraînement",elapsed){finish=false}
}

@Composable private fun SetInputDialog(vm:TrackerViewModel,runId:Long,step:WorkoutMovement,onClose:()->Unit) {
    var reps by remember{mutableStateOf(step.reps.toString())}
    var load by remember{mutableStateOf(fmt(step.loadKg,0))}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Série • ${step.name}")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Répétitions effectuées",reps,{reps=it},true)
            Field("Charge réelle (kg)",load,{load=it},true)
            Text("Le repos de ${step.restSec}s se déclenchera après validation.",style=MaterialTheme.typography.bodySmall)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val r=reps.toIntOrNull();val kg=double(load)
        if(r==null || r !in 0..300 || kg==null || kg !in 0.0..1000.0)error="Vérifie les répétitions et la charge"
        else{vm.recordLiveSet(runId,step,r,kg);onClose()}
    }){Text("Enregistrer")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun FinishWorkoutDialog(vm:TrackerViewModel,runId:Long,plan:String,elapsed:Long,onClose:()->Unit) {
    var calories by remember{mutableStateOf("")}
    var notes by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Bilan de séance")},text={
        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("Durée chronométrée : ${clockText(elapsed)}")
            Field("Calories dépensées (montre, facultatif)",calories,{calories=it},true)
            Field("Notes / ressenti",notes,{notes=it})
            Text("Les calories sont conservées séparément des repas. Champ vide = dépense non renseignée.",style=MaterialTheme.typography.bodySmall)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        if(vm.finishLiveWorkout(runId,plan,calories,notes))onClose()
        else error="Vérifie les calories et les notes"
    }){Text("Clôturer")}},dismissButton={TextButton(onClick=onClose){Text("Retour")}})
}
