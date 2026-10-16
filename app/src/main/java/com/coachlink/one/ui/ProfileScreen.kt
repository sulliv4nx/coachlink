package com.coachlink.one.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable fun ProfileScreen(vm:TrackerViewModel,p:Profile?,measurements:List<Measurement>,foods:List<Food>,logs:List<TrainingLog>,friends:List<FriendGoal>) {
    val profile=p?:Profile(age=0,sex="NA",heightCm=0.0,startWeightKg=0.0,goalWeightKg=0.0,goalDate="")
    var edit by remember {mutableStateOf(false)}
    var share by remember {mutableStateOf(false)}
    val weight=measurements.firstOrNull()?.weightKg ?: profile.startWeightKg
    val complete=profile.age in 18..100 && profile.heightCm in 100.0..240.0 && profile.startWeightKg in 25.0..350.0
    val needs=estimateNeeds(weight,profile.heightCm,profile.age,profile.sex,profile.activityFactor,profile.objective)
    val target=if(profile.calorieOverride in 1200..6000) profile.calorieOverride else needs?.targetKcal
    val total=foods.filter{it.date==today}.fold(zeroNutrition){acc,f->acc+toNutrition(f.kcal100,f.protein100,f.carbs100,f.fat100,f.grams)}
    Text("Bonjour, ${profile.name}",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
    Text("TON TABLEAU DE BORD",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
    if(!complete) Panel("COMMENCER TON SUIVI") {
        Text("Complète ton âge, ta taille, ton poids et ta date objectif pour activer les calculs et graphiques.")
        Button(onClick={edit=true},modifier=Modifier.fillMaxWidth()){Text("Compléter mon profil")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Metric("Poids actuel",if(weight>0)"${fmt(weight)} kg" else "—",MaterialTheme.colorScheme.primary,Modifier.weight(1f))
        Metric("IMC",bmi(weight,profile.heightCm)?.let{fmt(it)}?:"—",MaterialTheme.colorScheme.secondary,Modifier.weight(1f))
    }
    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Metric("Poids cible",if(profile.goalWeightKg>0)"${fmt(profile.goalWeightKg)} kg" else "—",MaterialTheme.colorScheme.tertiary,Modifier.weight(1f))
        Metric("Décompte",countdown(profile.goalDate)?.let{if(it>=0)"$it jours" else "Date passée"}?:"—",MaterialTheme.colorScheme.primary,Modifier.weight(1f))
    }
    Panel("OBJECTIF & PROGRESSION") {
        Text(objectiveLabel(profile.objective),fontWeight=FontWeight.Bold)
        if(complete && profile.goalWeightKg>0) {
            val fraction=progress(profile.startWeightKg,weight,profile.goalWeightKg)
            LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth().height(12.dp))
            Text("${(fraction*100).roundToInt()} % atteint · ${fmt(abs(weight-profile.goalWeightKg))} kg d'écart")
        }
        Button(onClick={edit=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Edit,null);Text(" Modifier objectif et profil")}
    }
    Panel("COURBE DE POIDS") {
        val weights=measurements.sortedBy{it.date}.takeLast(18).map{it.weightKg}
        if(weights.size<2) Text("Ajoute au moins deux pesées pour voir l'évolution.")
        else {
            val low=weights.min()+(-0.8);val high=weights.max()+0.8
            val lineColor=MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(145.dp)) {
                val path=Path()
                weights.forEachIndexed{i,w->
                    val x=i.toFloat()/(weights.size-1)*size.width
                    val y=size.height*(1f-((w-low)/(high-low)).toFloat())
                    if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
                }
                drawPath(path,lineColor,style=Stroke(width=3.dp.toPx()))
            }
            Text("${fmt(weights.first())} kg  →  ${fmt(weights.last())} kg",fontWeight=FontWeight.Bold)
        }
    }
    Panel("NUTRITION DU JOUR") {
        Text("${total.kcal.roundToInt()} kcal"+(target?.let{" / $it kcal"}?:" · besoin non configuré"),
            style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        if(target!=null) LinearProgressIndicator(progress={(total.kcal/target).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        Text("Protéines ${fmt(total.protein,0)} g • Glucides ${fmt(total.carbs,0)} g • Lipides ${fmt(total.fat,0)} g")
        Text("${logs.count{it.date==today}} exercice(s) enregistré(s) aujourd'hui",style=MaterialTheme.typography.bodySmall)
    }
    Panel("OBJECTIFS ENTRE AMIS") {
        Text("Envoie uniquement ton objectif et importe celui d'un ami ayant CoachLink One.")
        Button(onClick={share=true},enabled=complete,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.IosShare,null);Text(" Partager / importer")}
        friends.take(3).forEach{Text("${it.name} • ${objectiveLabel(it.objective)} • ${fmt(it.targetKg)} kg")}
    }
    Text("L'IMC ne distingue pas masse musculaire et masse grasse : à interpréter avec prudence.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(edit) ProfileDialog(vm,profile){edit=false}
    if(share) FriendsDialog(vm,profile,friends){share=false}
}

@Composable private fun ProfileDialog(vm:TrackerViewModel,p:Profile,onClose:()->Unit) {
    var name by remember {mutableStateOf(p.name)}
    var age by remember {mutableStateOf(if(p.age<=0)"" else p.age.toString())}
    var sex by remember {mutableStateOf(p.sex)}
    var height by remember {mutableStateOf(if(p.heightCm<=0)"" else fmt(p.heightCm))}
    var start by remember {mutableStateOf(if(p.startWeightKg<=0)"" else fmt(p.startWeightKg))}
    var target by remember {mutableStateOf(if(p.goalWeightKg<=0)"" else fmt(p.goalWeightKg))}
    var day by remember {mutableStateOf(p.goalDate)}
    var objective by remember {mutableStateOf(p.objective)}
    var activity by remember {mutableFloatStateOf(p.activityFactor.toFloat())}
    var calories by remember {mutableStateOf(if(p.calorieOverride==0)"" else p.calorieOverride.toString())}
    var error by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("PROFIL PERSONNEL")},text={
        Column(Modifier.heightIn(max=540.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Prénom",name,{name=it});Field("Âge",age,{age=it},true)
            Text("Sexe utilisé pour l'estimation énergétique",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("M" to "Masculin","F" to "Féminin").forEach{(v,l)->FilterChip(selected=sex==v,onClick={sex=v},label={Text(l)})}
            }
            Field("Taille (cm)",height,{height=it},true)
            Field("Poids de départ (kg)",start,{start=it},true)
            Field("Poids cible (kg)",target,{target=it},true)
            Field("Date de l'objectif (AAAA-MM-JJ)",day,{day=it})
            Text("Objectif",fontWeight=FontWeight.Bold)
            listOf("LOSS","GAIN","MAINTAIN").forEach{option->FilterChip(selected=objective==option,onClick={objective=option},label={Text(objectiveLabel(option))})}
            Text("Coefficient d'activité : ${fmt(activity.toDouble(),2)}")
            Slider(value=activity,onValueChange={activity=it},valueRange=1.2f..1.9f)
            Field("Calories cibles manuelles (facultatif)",calories,{calories=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val a=age.toIntOrNull();val h=double(height);val s=double(start);val t=double(target)
        val k=if(calories.isBlank())0 else calories.toIntOrNull()
        if(name.trim().length<2 || a==null || a !in 18..100 || sex !in listOf("M","F") ||
            h==null || h !in 100.0..240.0 || s==null || s !in 25.0..350.0 || t==null || t !in 25.0..350.0 ||
            !validDate(day) || k==null || (k!=0 && k !in 1200..6000)) error="Vérifie tes informations et la date"
        else {vm.saveProfile(p.copy(name=name.trim(),age=a,sex=sex,heightCm=h,startWeightKg=s,goalWeightKg=t,goalDate=day,
            objective=objective,activityFactor=activity.toDouble(),calorieOverride=k));onClose()}
    }){Text("Enregistrer")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}

@Composable private fun FriendsDialog(vm:TrackerViewModel,p:Profile,friends:List<FriendGoal>,onClose:()->Unit) {
    val ctx=LocalContext.current
    var pasted by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Partager mon objectif")},text={
        Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Partage choisi : prénom, objectif, poids cible, date. Les mesures, la nutrition et le sport restent privés.")
            Button(modifier=Modifier.fillMaxWidth(),onClick={
                val message="Mon objectif CoachLink One : ${objectiveLabel(p.objective)} • ${fmt(p.goalWeightKg)} kg • ${p.goalDate}\n\nCode à coller dans CoachLink One : ${GoalShare.encode(p)}"
                val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,message)}
                ctx.startActivity(Intent.createChooser(intent,"Partager l'objectif"))
            }){Text("Envoyer par le partage Android")}
            OutlinedTextField(value=pasted,onValueChange={pasted=it},label={Text("Coller un code reçu")},minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
            OutlinedButton(onClick={val f=GoalShare.decode(pasted);if(f==null)error="Code non reconnu" else{vm.addFriend(f);pasted="";error=""}}){Text("Importer l'objectif d'un ami")}
            friends.forEach{f->Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("${f.name} : ${fmt(f.targetKg)} kg",modifier=Modifier.weight(1f));IconButton(onClick={vm.removeFriend(f.id)}){Icon(Icons.Default.Delete,null)}}}
            Text("Échange manuel entre téléphones : aucune synchronisation en direct dans cette version.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick=onClose){Text("Fermer")}})
}
