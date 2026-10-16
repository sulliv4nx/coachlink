package com.coachlink.one.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
import kotlin.math.roundToInt

/** Fast set logging without opening a dialog between sets. */
@Composable
fun QuickSetRecorder(vm:TrackerViewModel,run:WorkoutRun,move:WorkoutMovement,
    allResults:List<WorkoutSetResult>,enabled:Boolean,onSkip:()->Unit) {
    val current = allResults.filter { it.runId==run.id && it.movementId==move.id }
    val previous=allResults.filter {it.status=="DONE" && it.movementName.equals(move.name,ignoreCase=true) &&
        it.runId!=run.id && it.recordedAtMs<=run.startedAtMs}.maxByOrNull {it.recordedAtMs}
    val latest = current.lastOrNull{it.status=="DONE"}
    var kg by remember(move.id,run.id) {mutableStateOf(fmt(previous?.loadKg?:move.loadKg,1))}
    var reps by remember(move.id,run.id) {mutableStateOf((previous?.reps?:move.reps).toString())}
    var rpe by remember(move.id,run.id) {mutableStateOf("")}
    var setType by remember(move.id,run.id) {mutableStateOf("NORMAL")}
    var error by remember(move.id,run.id) {mutableStateOf("")}
    val completed=current.count{it.status=="DONE"}
    val estimates=allResults.filter{it.status=="DONE" && it.movementName.equals(move.name,true)}
        .map{LiftSample(it.movementName,it.reps,it.loadKg,it.recordedAtMs,it.setType)}
    val stats=liftStats(estimates,move.name)

    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(move.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,
                modifier=Modifier.weight(1f))
            if(move.supersetGroup.isNotBlank())
                Text("SUPERSET "+move.supersetGroup,color=MaterialTheme.colorScheme.tertiary,
                    style=MaterialTheme.typography.labelMedium)
        }
        Text("Prévu : ${move.sets} × ${move.reps} • ${fmt(move.loadKg,1)} kg • repos ${move.restSec}s",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${completed} / ${move.sets} séries consignées",
            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
        if(previous!=null) {
            Text("DERNIÈRE FOIS  ${fmt(previous.loadKg,1)} kg × ${previous.reps}",
                color=MaterialTheme.colorScheme.tertiary,style=MaterialTheme.typography.labelMedium)
        }
        if(stats!=null) Text("Record : ${fmt(stats.bestKg,1)} kg   ·   1RM estimé : ${fmt(stats.estimatedOneRm,1)} kg",
            style=MaterialTheme.typography.bodySmall)
        if(current.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Série",style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(.7f))
                Text("Charge",style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
                Text("Rép.",style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
                Text("Type",style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
            }
            current.forEach { entry ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(entry.setNumber.toString(),modifier=Modifier.weight(.7f))
                    Text(if(entry.status=="SKIP")"—" else fmt(entry.loadKg,1),modifier=Modifier.weight(1f))
                    Text(if(entry.status=="SKIP")"—" else entry.reps.toString(),modifier=Modifier.weight(1f))
                    Text(if(entry.status=="SKIP")"Passé" else entry.setType.take(5),modifier=Modifier.weight(1f))
                }
            }
        }
        Text("SÉRIE SUIVANTE",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value=kg,onValueChange={kg=it;error=""},modifier=Modifier.weight(1f),
                label={Text("kg")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            OutlinedTextField(value=reps,onValueChange={reps=it;error=""},modifier=Modifier.weight(1f),
                label={Text("Rép.")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            OutlinedTextField(value=rpe,onValueChange={rpe=it.take(4);error=""},modifier=Modifier.weight(1f),
                label={Text("RPE")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf("NORMAL" to "Normal","WARMUP" to "Échauff.",
                "DROPSET" to "Dégressive","FAILURE" to "Échec").forEach { (value,label) ->
                FilterChip(selected=setType==value,onClick={setType=value},label={Text(label)})
            }
        }
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={
                val weight=double(kg)
                val count=reps.toIntOrNull()
                val effort=if(rpe.isBlank())null else double(rpe)
                if(weight==null || weight !in 0.0..1000.0 || count==null || count !in 0..300 ||
                    (rpe.isNotBlank() && (effort==null || effort !in 1.0..10.0)))
                    error="Vérifie la charge, les répétitions et le RPE."
                else {vm.recordLiveSet(run.id,move,count,weight,setType=setType,rpe=effort);error=""}
            },enabled=enabled,modifier=Modifier.weight(1f)) {Text("✓ Valider la série")}
            OutlinedButton(onClick=onSkip,enabled=enabled) {Text("Passer")}
        }
    }
}
