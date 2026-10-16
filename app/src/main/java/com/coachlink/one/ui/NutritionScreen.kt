package com.coachlink.one.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coachlink.one.data.*
import com.coachlink.one.domain.*
import kotlin.math.roundToInt

@Composable fun NutritionScreen(vm:TrackerViewModel,profile:Profile?,measurements:List<Measurement>,foods:List<Food>) {
    var adding by remember {mutableStateOf(false)}
    val date=today
    val day=foods.filter{it.date==date}
    val totals=day.fold(zeroNutrition){sum,f->sum+toNutrition(f.kcal100,f.protein100,f.carbs100,f.fat100,f.grams)}
    val p=profile
    val needs=if(p==null)null else estimateNeeds(measurements.firstOrNull()?.weightKg?:p.startWeightKg,p.heightCm,p.age,p.sex,p.activityFactor,p.objective)
    val target=if(p!=null && p.calorieOverride in 1200..6000)p.calorieOverride else needs?.targetKcal
    Text("Nutrition",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
    Text("Repas, encas et shakers",color=MaterialTheme.colorScheme.onSurfaceVariant)
    Panel("BILAN CALORIQUE • AUJOURD'HUI") {
        Text("${totals.kcal.roundToInt()} kcal"+(target?.let{" / $it kcal"}?:""),style=MaterialTheme.typography.headlineMedium,
            color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
        if(target!=null) LinearProgressIndicator(progress={(totals.kcal/target).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(11.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Protéines ${fmt(totals.protein,0)} g");Text("Glucides ${fmt(totals.carbs,0)} g")}
        Text("Lipides ${fmt(totals.fat,0)} g")
        needs?.let{
            HorizontalDivider()
            Text("Besoins estimés : maintien ${it.maintenanceKcal} kcal • cible ${target ?: it.targetKcal} kcal",fontWeight=FontWeight.Bold)
            Text("Macros indicatives : protéines ${it.proteinG} g · glucides ${it.carbsG} g · lipides ${it.fatG} g",style=MaterialTheme.typography.bodySmall)
        } ?: Text("Complète ton profil pour estimer les besoins journaliers.",style=MaterialTheme.typography.bodySmall)
    }
    Button(onClick={adding=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" Ajouter un aliment / encas / shaker")}
    Panel("JOURNAL ALIMENTAIRE") {
        if(day.isEmpty())Text("Aucun repas enregistré aujourd'hui.")
        day.forEach{f->
            val n=toNutrition(f.kcal100,f.protein100,f.carbs100,f.fat100,f.grams)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)){
                    Text("${f.kind} • ${f.name}",fontWeight=FontWeight.Bold)
                    Text("${fmt(f.grams,0)} g · ${fmt(n.kcal,0)} kcal · P ${fmt(n.protein,0)} g",style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick={vm.removeFood(f.id)}){Icon(Icons.Default.DeleteOutline,"Retirer")}
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
    }
    Panel("7 DERNIERS JOURS") {
        val from=java.time.LocalDate.now().minusDays(6)
        val week=foods.filter{runCatching{val d=java.time.LocalDate.parse(it.date);!d.isBefore(from)&&!d.isAfter(java.time.LocalDate.now())}.getOrDefault(false)}
        val days=week.map{it.date}.distinct().size
        val sum=week.fold(zeroNutrition){s,f->s+toNutrition(f.kcal100,f.protein100,f.carbs100,f.fat100,f.grams)}
        Text("$days jour(s) renseigné(s) · Moyenne sur jours saisis : ${if(days>0)fmt(sum.kcal/days,0) else "—"} kcal")
        Text("Les jours vides ne sont pas comptés comme des jours à zéro calorie.",style=MaterialTheme.typography.bodySmall)
    }
    if(adding) NutritionDialog(vm){adding=false}
}
@Composable private fun NutritionDialog(vm:TrackerViewModel,onClose:()->Unit) {
    var name by remember{mutableStateOf("")}
    var kind by remember{mutableStateOf(mealKinds[1])}
    var grams by remember{mutableStateOf("100")}
    var kcal by remember{mutableStateOf("")}
    var protein by remember{mutableStateOf("")}
    var carbs by remember{mutableStateOf("")}
    var fat by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Ajouter au journal")},text={
        Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                mealKinds.forEach{v->FilterChip(selected=kind==v,onClick={kind=v},label={Text(v)})}
            }
            Field("Nom de l'aliment",name,{name=it})
            Field("Quantité consommée (g)",grams,{grams=it},true)
            Text("Valeurs pour 100 g (étiquette produit)",fontWeight=FontWeight.Bold)
            Field("Calories / 100 g",kcal,{kcal=it},true)
            Field("Protéines / 100 g",protein,{protein=it},true)
            Field("Glucides / 100 g",carbs,{carbs=it},true)
            Field("Lipides / 100 g",fat,{fat=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val g=double(grams);val values=listOf(double(kcal),double(protein),double(carbs),double(fat))
        if(name.trim().length !in 2..100 || g==null || g !in 1.0..5000.0 || values.any{it==null || it !in 0.0..1000.0}) error="Vérifie quantités et nutrition pour 100 g"
        else {vm.addFood(Food(date=today,kind=kind,name=name.trim(),grams=g,kcal100=values[0]!!,protein100=values[1]!!,carbs100=values[2]!!,fat100=values[3]!!));onClose()}
    }){Text("Ajouter")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}
