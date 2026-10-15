package com.coachlink.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coachlink.app.data.Meal
import com.coachlink.app.data.MealPlanIngredient
import com.coachlink.app.domain.*
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

private val dayLabels = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

@Composable
fun GroceryScreen(vm: CoachViewModel, planned: List<MealPlanIngredient>, meals: List<Meal>) {
    var mode by remember { mutableStateOf("week") }
    var add by remember { mutableStateOf(false) }
    var confirmCopy by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    val today = LocalDate.now()
    val from = if (mode == "week") today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
               else today.withDayOfMonth(1)
    val until = if (mode == "week") from.plusDays(6) else from.withDayOfMonth(from.lengthOfMonth())
    val groceries = remember(planned, from, until) {
        buildGroceryList(planned.map { PlannedIngredient(it.weekday, it.mealType, it.food, it.grams, it.servings) }, from, until)
    }
    val checkIdentity = "$mode:$from:${planned.hashCode()}"
    LaunchedEffect(checkIdentity) { checked = emptySet() }

    Text("Menus & liste de courses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Les courses proviennent des repas prévus, pas de l'estimation du contenu de vos placards. Le mois répète la trame hebdomadaire.",
        style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = mode == "week", onClick = { mode = "week" }, label = { Text("Semaine") })
        FilterChip(selected = mode == "month", onClick = { mode = "month" }, label = { Text("Mois") })
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Repas programmés par jour", fontWeight = FontWeight.Bold)
            Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Prévoir un aliment / ingrédient") }
            OutlinedButton(onClick = { confirmCopy = true }, modifier = Modifier.fillMaxWidth()) { Text("Reprendre mes aliments des 7 derniers jours") }
            Text("La reprise remplace le menu actuel. Les portions photographiées peuvent être approximatives.",
                style = MaterialTheme.typography.bodySmall)
            if (planned.isEmpty()) Text("Aucun menu pour l'instant. Ajoutez des aliments pour créer la liste de courses.")
            planned.groupBy { it.weekday }.toSortedMap().forEach { (day, entries) ->
                Text(dayLabels.getOrElse(day - 1) { "Jour $day" }, style = MaterialTheme.typography.titleSmall)
                entries.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.food, fontWeight = FontWeight.Medium)
                            Text("${item.mealType} • ${gramsDisplay(item.grams)} × ${item.servings} portion(s)", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.removePlannedIngredient(item.id) }) { Text("Retirer") }
                    }
                }
            }
        }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Courses du $from au $until", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${groceries.plannedDays} jours • ${groceries.items.size} aliment(s) différents")
            if (groceries.items.isEmpty()) Text("Votre liste apparaît dès que vous prévoyez des repas.")
            groceries.items.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = line.key in checked, onCheckedChange = { value ->
                        checked = if (value) checked + line.key else checked - line.key
                    })
                    Column(Modifier.padding(top = 8.dp)) {
                        Text("${line.label} — ${gramsDisplay(line.grams)}")
                        Text("${line.servings} portion(s) cumulées", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Pensez à corriger les stocks déjà à la maison, les déchets de préparation et les unités à l'achat. Les cases cochées ne sont pas encore synchronisées.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (add) AddIngredientDialog(vm) { add = false }
    if (confirmCopy) AlertDialog(onDismissRequest = { confirmCopy = false },
        title = { Text("Créer le menu depuis l'historique ?") },
        text = { Text("Les repas enregistrés sur les 7 derniers jours remplaceront les repas prévus. Vérifiez les quantités avant de faire les courses.") },
        confirmButton = { Button(onClick = { vm.copyRecentMealsToPlan(meals); confirmCopy = false }) { Text("Remplacer mon menu") } },
        dismissButton = { TextButton(onClick = { confirmCopy = false }) { Text("Annuler") } })
}

@Composable
private fun AddIngredientDialog(vm: CoachViewModel, close: () -> Unit) {
    var day by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    var mealType by remember { mutableStateOf("Déjeuner") }
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("150") }
    var servings by remember { mutableStateOf("1") }
    AlertDialog(onDismissRequest = close, title = { Text("Prévoir un aliment") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jour de la semaine")
            dayLabels.indices.chunked(4).forEach { group ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.forEach { idx -> FilterChip(selected = day == idx + 1, onClick = { day = idx + 1 }, label = { Text(dayLabels[idx]) }) }
                }
            }
            Text("Repas")
            listOf("Petit-déjeuner", "Déjeuner", "Dîner", "Collation").chunked(2).forEach { pair ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pair.forEach { label -> FilterChip(selected = mealType == label, onClick = { mealType = label }, label = { Text(label) }) }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Aliment (ex : riz cru)") }, singleLine = true)
            OutlinedTextField(value = grams, onValueChange = { grams = it }, label = { Text("Quantité par portion (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = servings, onValueChange = { servings = it }, label = { Text("Nombre de portions") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Utilisez un nom cohérent (cru ou cuit), sinon les quantités cumulées seront trompeuses.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { Button(onClick = {
        if (vm.addPlannedIngredient(day, mealType, name, grams.replace(',', '.').toDoubleOrNull() ?: -1.0, servings.toIntOrNull() ?: -1)) close()
    }) { Text("Ajouter") } }, dismissButton = { TextButton(onClick = close) { Text("Annuler") } })
}
