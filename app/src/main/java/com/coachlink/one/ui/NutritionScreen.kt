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
    if(adding) NutritionDialog(vm,foods){adding=false}
}
@Composable private fun NutritionDialog(vm:TrackerViewModel,foods:List<Food>,onClose:()->Unit) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(mealKinds[1]) }
    var grams by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("Saisie manuelle") }
    var error by remember { mutableStateOf("") }
    val suggestions by vm.suggestions.collectAsState()
    val busy by vm.foodSearchBusy.collectAsState()
    val recent = foods.distinctBy { it.name.lowercase() }.take(6)
    val quantity = double(grams)
    val macros = listOf(double(kcal), double(protein), double(carbs), double(fat))
    val preview = if (quantity != null && quantity in 0.1..5000.0 && macros.all { it != null && it >= 0 })
        toNutrition(macros[0]!!, macros[1]!!, macros[2]!!, macros[3]!!, quantity) else null
    fun fill(foodName:String,k:Double,p:Double,c:Double,f:Double,from:String) {
        name=foodName;kcal=fmt(k,1);protein=fmt(p,1);carbs=fmt(c,1);fat=fmt(f,1)
        source=from;error="";vm.clearSuggestions()
    }
    AlertDialog(onDismissRequest=onClose,title={Text("Ajouter au journal")},text={
        Column(Modifier.heightIn(max=540.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                mealKinds.forEach{v->FilterChip(selected=kind==v,onClick={kind=v},label={Text(v)})}
            }
            Text("1. Cherche l'aliment, sélectionne la fiche, puis indique les grammes.",style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(value=name,onValueChange={ name=it;source="Saisie manuelle";vm.clearSuggestions() },
                modifier=Modifier.fillMaxWidth(),label={Text("Aliment ou marque")},singleLine=true)
            Button(onClick={vm.searchFoods(name)},enabled=name.trim().length>=3 && !busy,modifier=Modifier.fillMaxWidth()) {
                Text(if(busy) "Recherche en cours..." else "Chercher et calculer automatiquement")
            }
            if(suggestions.isEmpty() && !busy) Text("Produits emballés : données Open Food Facts. Vérifie la fiche sélectionnée.",style=MaterialTheme.typography.bodySmall)
            suggestions.forEach { option ->
                OutlinedButton(onClick={fill(option.name,option.kcal100,option.protein100,option.carbs100,option.fat100,option.source)},
                    modifier=Modifier.fillMaxWidth()) {
                    Column {
                        Text((option.name+" • "+option.brand).take(100),fontWeight=FontWeight.Bold)
                        Text("${fmt(option.kcal100,0)} kcal/100 g · P ${fmt(option.protein100,0)} g · G ${fmt(option.carbs100,0)} g",
                            style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if(recent.isNotEmpty()) {
                Text("MES ALIMENTS RÉCENTS",fontWeight=FontWeight.Bold)
                recent.forEach { food ->
                    TextButton(onClick={fill(food.name,food.kcal100,food.protein100,food.carbs100,food.fat100,"Historique personnel")}) {
                        Text(food.name)
                    }
                }
            }
            Field("2. Quantité consommée (g)",grams,{grams=it},true)
            preview?.let { totals ->
                Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color=MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${fmt(totals.kcal,0)} kcal pour ${fmt(quantity!!,0)} g",
                            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                        Text("Protéines ${fmt(totals.protein,1)} g • Glucides ${fmt(totals.carbs,1)} g • Lipides ${fmt(totals.fat,1)} g",
                            style=MaterialTheme.typography.bodySmall)
                        Text(source,style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("Valeurs pour 100 g — vérifiables et modifiables",fontWeight=FontWeight.Bold)
            Field("Calories / 100 g",kcal,{kcal=it;source="Valeurs ajustées manuellement"},true)
            Field("Protéines / 100 g",protein,{protein=it;source="Valeurs ajustées manuellement"},true)
            Field("Glucides / 100 g",carbs,{carbs=it;source="Valeurs ajustées manuellement"},true)
            Field("Lipides / 100 g",fat,{fat=it;source="Valeurs ajustées manuellement"},true)
            Text("Un nom seul ne suffit pas à connaître la recette ni les quantités. Pour un plat maison, utilise les valeurs des ingrédients ou saisis celles de l'étiquette.",
                style=MaterialTheme.typography.bodySmall)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val g=double(grams);val values=listOf(double(kcal),double(protein),double(carbs),double(fat))
        if(name.trim().length !in 2..100 || g==null || !g.isFinite() || g !in 1.0..5000.0 ||
            values.any { it==null || !it.isFinite() || it !in 0.0..1000.0 } ||
            values.drop(1).any{it!!>100.0}) error="Vérifie quantité et valeurs pour 100 g"
        else {
            vm.addFood(Food(date=today,kind=kind,name=name.trim(),grams=g,
                kcal100=values[0]!!,protein100=values[1]!!,carbs100=values[2]!!,fat100=values[3]!!))
            vm.clearSuggestions();onClose()
        }
    }){Text("Ajouter ${preview?.let{fmt(it.kcal,0)+" kcal"}?:""}")}},
        dismissButton={TextButton(onClick={onClose}){Text("Annuler")}})
}
