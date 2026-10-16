package com.coachlink.one.ui

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
import com.coachlink.one.domain.bmi

@Composable fun MeasuresScreen(vm:TrackerViewModel,profile:Profile?,items:List<Measurement>) {
    var adding by remember {mutableStateOf(false)}
    Text("Mensurations",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
    Text("Pesées et mesures corporelles",color=MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick={adding=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" Ajouter une mesure")}
    val latest=items.firstOrNull()
    if(latest!=null) {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Metric("Poids", "${fmt(latest.weightKg)} kg",MaterialTheme.colorScheme.primary,Modifier.weight(1f))
            Metric("IMC",profile?.let{bmi(latest.weightKg,it.heightCm)}?.let{fmt(it)}?:"—",MaterialTheme.colorScheme.secondary,Modifier.weight(1f))
        }
    }
    Panel("HISTORIQUE DES MESURES") {
        if(items.isEmpty())Text("Aucune mesure pour l'instant.")
        items.forEach{m->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("${m.date} • ${fmt(m.weightKg)} kg",fontWeight=FontWeight.Bold)
                    val values=listOfNotNull(m.waistCm?.let{"Taille ${fmt(it)}"},m.chestCm?.let{"Poitrine ${fmt(it)}"},
                        m.hipsCm?.let{"Hanches ${fmt(it)}"},m.armCm?.let{"Bras ${fmt(it)}"},m.thighCm?.let{"Cuisse ${fmt(it)}"})
                    if(values.isNotEmpty())Text(values.joinToString(" · ")+" cm",style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick={vm.removeMeasurement(m.id)}){Icon(Icons.Default.DeleteOutline,"Supprimer")}
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
    }
    if(adding) MeasureDialog(vm){adding=false}
}
@Composable private fun MeasureDialog(vm:TrackerViewModel,onClose:()->Unit) {
    var date by remember {mutableStateOf(today)}
    var weight by remember {mutableStateOf("")}
    var waist by remember {mutableStateOf("")}
    var chest by remember {mutableStateOf("")}
    var hips by remember {mutableStateOf("")}
    var arm by remember {mutableStateOf("")}
    var thigh by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onClose,title={Text("Nouvelle mensuration")},text={
        Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Field("Date (AAAA-MM-JJ)",date,{date=it})
            Field("Poids (kg)",weight,{weight=it},true)
            Field("Tour de taille (cm, facultatif)",waist,{waist=it},true)
            Field("Poitrine (cm, facultatif)",chest,{chest=it},true)
            Field("Hanches (cm, facultatif)",hips,{hips=it},true)
            Field("Bras (cm, facultatif)",arm,{arm=it},true)
            Field("Cuisse (cm, facultatif)",thigh,{thigh=it},true)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={
        val w=double(weight)
        val optional=listOf(waist,chest,hips,arm,thigh)
        if(!validDate(date)|| w==null || w !in 25.0..350.0 || optional.any{it.isNotBlank() && (double(it)==null || (double(it) ?: 0.0) !in 10.0..250.0)}) error="Valeurs ou date incorrectes"
        else {vm.addMeasurement(Measurement(date=date,weightKg=w,waistCm=double(waist),chestCm=double(chest),hipsCm=double(hips),armCm=double(arm),thighCm=double(thigh)));onClose()}
    }){Text("Enregistrer")}},dismissButton={TextButton(onClick=onClose){Text("Annuler")}})
}
