package com.coachlink.one.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import kotlin.math.roundToInt

/** Progress uses real logged sets; no extrapolation from uncompleted plans. */
@Composable
fun StrengthProgressScreen(sets:List<WorkoutSetResult>) {
    val completed=sets.filter{it.status=="DONE" && it.reps>0 && it.loadKg>=0 && it.loadKg.isFinite()}
    val names=completed.map{it.movementName.trim()}.distinctBy{it.lowercase()}.sorted()
    var selected by remember {mutableStateOf("")}
    val selectedName=if(selected in names)selected else names.firstOrNull().orEmpty()
    val samples=completed.map{LiftSample(it.movementName,it.reps,it.loadKg,it.recordedAtMs,it.setType)}
    val stats=liftStats(samples,selectedName)
    Text("PROGRESSION & RECORDS",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
    Text("Comme dans Strong et Hevy : chaque série terminée alimente ton historique.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Panel("TON HISTORIQUE DE FORCE") {
        if(names.isEmpty()) {
            Text("Aucune série en mode direct. Lance une séance depuis Planning & Live pour voir tes premiers records.")
        } else {
            Text("Choisir un exercice",fontWeight=FontWeight.Bold)
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                names.chunked(3).take(12).forEach { row ->
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        row.forEach { n->
                            FilterChip(selected=selectedName==n,onClick={selected=n},
                                label={Text(n,maxLines=1)})
                        }
                    }
                }
            }
            if(stats!=null) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Metric("Meilleure charge","${fmt(stats.bestKg,1)} kg",
                        MaterialTheme.colorScheme.primary,Modifier.weight(1f))
                    Metric("1RM estimé","${fmt(stats.estimatedOneRm,1)} kg",
                        MaterialTheme.colorScheme.secondary,Modifier.weight(1f))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Metric("Volume total","${fmt(stats.totalVolumeKg,0)} kg",
                        MaterialTheme.colorScheme.tertiary,Modifier.weight(1f))
                    Metric("Séries","${stats.totalSets}",
                        MaterialTheme.colorScheme.primary,Modifier.weight(1f))
                }
                Text("ÉVOLUTION DE LA CHARGE PAR SÉRIE",fontWeight=FontWeight.Bold)
                val points=stats.history.takeLast(24)
                val min=points.minOf{it.kg}
                val max=points.maxOf{it.kg}
                val color=MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                    val path=Path()
                    val range=(max-min).coerceAtLeast(2.0)
                    points.forEachIndexed{index,p->
                        val x=if(points.size==1)size.width/2f else index.toFloat()/(points.size-1)*size.width
                        val y=size.height * (0.90f-0.80f*((p.kg-min)/range).toFloat())
                        if(index==0) path.moveTo(x,y) else path.lineTo(x,y)
                        drawCircle(color,radius=3.dp.toPx(),center=Offset(x,y))
                    }
                    if(points.size>1)drawPath(path,color,style=Stroke(2.dp.toPx()))
                }
                Text("Début : ${fmt(points.first().kg,1)} kg  ·  Dernière : ${fmt(points.last().kg,1)} kg",
                    style=MaterialTheme.typography.bodySmall)
                Text("Le 1RM utilise uniquement les séries de 1 à 10 répétitions. Il reste une estimation, pas un record testé.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Panel("VOLUME DES 7 DERNIERS JOURS") {
        val zone=ZoneId.systemDefault()
        val end=LocalDate.now()
        (6 downTo 0).forEach {daysAgo ->
            val day=end.minusDays(daysAgo.toLong())
            val reps=completed.filter{
                Instant.ofEpochMilli(it.recordedAtMs).atZone(zone).toLocalDate()==day
            }
            val volume=reps.sumOf {trainingVolume(it.loadKg,it.reps)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(day.toString().takeLast(5))
                Text("${reps.size} séries · ${volume.roundToInt()} kg")
            }
        }
        Text("Volume = charge × répétitions, uniquement pour les mouvements chargés et consignés.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
