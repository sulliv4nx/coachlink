package com.coachlink.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.coachlink.app.data.*
import com.coachlink.app.domain.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun muscle(value: String): Muscle? = Muscle.entries.find { it.name == value }
private fun objective(value: String): TrainingObjective = TrainingObjective.entries.find { it.name == value } ?: TrainingObjective.FITNESS
private fun mode(value: String): CoachingMode = CoachingMode.entries.find { it.name == value } ?: CoachingMode.AI
private fun numberOrNull(value: String) = value.replace(',', '.').toDoubleOrNull()

/** The coach settings affect training and nutrition independently, with explicit athlete validation. */
@Composable
fun AdaptiveCoachScreen(vm: CoachViewModel, athleteSettings: AthleteSettings, nutrition: Goal, meals: List<Meal>,
                        logs: List<WorkoutLog>, exercises: List<Exercise>, extra: List<ExtraWorkout>,
                        crossfit: List<CrossFitSession>, metrics: List<BodyMetric>, isHumanCoach: Boolean) {
    val currentObjective = objective(athleteSettings.objective)
    var sport by remember(athleteSettings) { mutableStateOf(mode(athleteSettings.sportMode)) }
    var food by remember(athleteSettings) { mutableStateOf(mode(athleteSettings.nutritionMode)) }
    var selectedObjective by remember(athleteSettings) { mutableStateOf(currentObjective) }
    var weak by remember(athleteSettings) { mutableStateOf(muscle(athleteSettings.weakMuscle)) }
    var sessions by remember(athleteSettings) { mutableIntStateOf(athleteSettings.sessionsPerWeek) }
    var addingExtra by remember { mutableStateOf(false) }
    var serverConsent by remember { mutableStateOf(false) }
    val aiAdvice by vm.aiAdvice.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val today = LocalDate.now()
    val start = today.minusDays(6)
    val recentMeals = meals.filter { !it.dayDate().isBefore(start) && !it.dayDate().isAfter(today) }
    val recordedDays = recentMeals.map { it.day }.distinct().size
    val review = reviewNutrition(today, MacroTarget(nutrition.calories, nutrition.protein, nutrition.carbs, nutrition.fat),
        recentMeals.map { DailyNutrition(it.dayDate(), it.calories100 * it.grams / 100,
            it.protein100 * it.grams / 100) }, mode(athleteSettings.nutritionMode) == CoachingMode.AI)
    val proteinAvg = review.avgProtein
    val workoutHistory = remember(logs, exercises, extra, crossfit) {
        val exerciseById = exercises.associateBy { it.id }
        val programmed = logs.mapNotNull { log ->
            val ex = exerciseById[log.exerciseId] ?: return@mapNotNull null
            ActivityInput(ex.name, log.dayDate(), detectMuscle(ex.name) ?: muscle(ex.muscleGroup) ?: Muscle.CORE,
                log.setsDone, log.repsDone, log.perceivedEffort, source = "programme")
        }
        val outside = extra.map { ActivityInput(it.name, it.dayDate(), muscle(it.muscleGroup) ?: Muscle.CORE,
            it.sets, it.reps, it.rpe, it.pain, source = "hors programme") }
        val actualCrossfit = crossfit.flatMap { wod ->
            val areas = wod.muscleGroups.split(",").mapNotNull { muscle(it.trim()) }.toSet()
            crossfitActivities(runCatching { LocalDate.parse(wod.day) }.getOrDefault(LocalDate.MIN),
                wod.title, wod.durationMinutes, wod.rpe, areas, wod.pain)
        }
        programmed + outside + actualCrossfit
    }
    val proposal = remember(athleteSettings, nutrition, logs, exercises, extra, crossfit, meals, metrics) {
        proposeSession(today, AthleteInputs(currentObjective, muscle(athleteSettings.weakMuscle),
            athleteSettings.sessionsPerWeek, metrics.firstOrNull()?.weightKg,
            MacroTarget(nutrition.calories, nutrition.protein, nutrition.carbs, nutrition.fat), proteinAvg, recordedDays),
            workoutHistory)
    }

    Text("Coach IA & objectifs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Chaque entraînement déclaré actualise la prochaine recommandation. Les décisions restent vérifiables et modifiables.",
        style = MaterialTheme.typography.bodySmall)

    CoachPanel("Choisir son accompagnement") {
        Text("Sport", fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoachingMode.entries.forEach { item ->
                FilterChip(selected = sport == item, onClick = { sport = item }, label = { Text(if (item == CoachingMode.AI) "Coach IA" else "Coach humain") })
            }
        }
        Text("Nutrition", fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoachingMode.entries.forEach { item ->
                FilterChip(selected = food == item, onClick = { food = item }, label = { Text(if (item == CoachingMode.AI) "Coach IA" else "Coach humain") })
            }
        }
        Text("Objectif principal", fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            TrainingObjective.entries.chunked(3).forEach { group ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    group.forEach { item -> FilterChip(selected = selectedObjective == item,
                        onClick = { selectedObjective = item }, label = { Text(item.label) }) }
                }
            }
        }
        Text("Point faible à renforcer (si disponible)", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = weak == null, onClick = { weak = null }, label = { Text("Aucun") })
        }
        Muscle.entries.chunked(3).forEach { group ->
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                group.forEach { item -> FilterChip(selected = weak == item, onClick = { weak = item }, label = { Text(item.label) }) }
            }
        }
        Text("Fréquence : $sessions séance(s) / semaine")
        Slider(value = sessions.toFloat(), onValueChange = { sessions = it.toInt().coerceIn(1, 7) },
            valueRange = 1f..7f, steps = 5)
        Button(onClick = { vm.saveSettings(sport, food, selectedObjective, weak, sessions) }, modifier = Modifier.fillMaxWidth()) {
            Text("Enregistrer mes choix")
        }
        if (isHumanCoach) Text("Mode coach de démonstration sur cet appareil : pas encore de comptes distincts.", style = MaterialTheme.typography.bodySmall)
    }

    CoachPanel("CROSSFIT • COURS PRIS EN COMPTE") {
        if (crossfit.isEmpty()) Text("Aucun WOD encore renseigné : tu peux le saisir dans Sport.")
        crossfit.take(5).forEach { wod ->
            Text(wod.day + " • " + wod.title + " • " + wod.durationMinutes + " min • RPE " + wod.rpe, fontWeight = FontWeight.Bold)
            Text("Zones : " + wod.muscleGroups.split(",").mapNotNull { muscle(it.trim())?.label }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall)
        }
        Text("L'algorithme tient compte des muscles confirmés et de la difficulté du WOD pour limiter les répétitions de charge.", style = MaterialTheme.typography.bodySmall)
    }
    CoachPanel("Activités libres / hors programmation") {
        Text("Saisir un exercice déjà réalisé pour que la prochaine séance tienne compte des muscles sollicités.")
        Button(onClick = { addingExtra = true }, modifier = Modifier.fillMaxWidth()) { Text("+ J'ai fait une séance hors programme") }
        if (extra.isEmpty()) Text("Aucune activité libre saisie.")
        extra.take(8).forEach { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("${item.name} • ${muscle(item.muscleGroup)?.label ?: "Autre"}", fontWeight = FontWeight.SemiBold)
                    Text("${item.day} • ${item.sets} × ${item.reps} • RPE ${item.rpe}${if (item.pain) " • douleur signalée" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { vm.removeExtra(item.id) }) { Text("Retirer") }
            }
            HorizontalDivider()
        }
    }
    CoachPanel(if (mode(athleteSettings.sportMode) == CoachingMode.AI) "Séance adaptative proposée" else "Aperçu facultatif d'adaptation") {
        Text(proposal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        proposal.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
        if (proposal.exercises.isEmpty()) Text("Repos ou récupération légère conseillée : pas de séance à ajouter.")
        proposal.exercises.forEach { ex ->
            Text("• ${ex.name} — ${ex.sets} séries × ${ex.reps} répétitions, repos ${ex.restSec}s")
        }
        proposal.explanation.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        if (mode(athleteSettings.sportMode) == CoachingMode.AI) {
            Button(onClick = { vm.acceptProposal(proposal) }, enabled = proposal.exercises.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Valider et ajouter à mes programmes")
            }
        } else Text("Le programme de votre coach reste prioritaire. Vous pouvez saisir ses séances dans l'onglet Sport.")
    }
    CoachPanel("Analyse approfondie par IA (facultative)") {
        Text("Le moteur local adapte les séances sans connexion. Le service IA en ligne peut expliquer la progression et proposer des pistes supplémentaires.")
        if (!vm.coachOnlineConfigured) Text("Service IA non configuré : l'adaptation locale est opérationnelle.")
        else {
            Row {
                Checkbox(checked = serverConsent, onCheckedChange = { serverConsent = it })
                Text("J'accepte l'envoi d'un résumé de mes objectifs, macros et entraînements au service IA.", modifier = Modifier.padding(top = 9.dp))
            }
            Button(onClick = {
                vm.requestAiReview(today, AthleteInputs(currentObjective, muscle(athleteSettings.weakMuscle),
                    athleteSettings.sessionsPerWeek, metrics.firstOrNull()?.weightKg,
                    MacroTarget(nutrition.calories, nutrition.protein, nutrition.carbs, nutrition.fat), proteinAvg, recordedDays),
                    workoutHistory)
            }, enabled = serverConsent && !aiBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (aiBusy) "Analyse en cours…" else "Demander mon bilan IA")
            }
        }
        aiAdvice?.let {
            Text(it.overview, fontWeight = FontWeight.SemiBold)
            Text("Sport : ${it.training}")
            Text("Nutrition : ${it.nutrition}")
            Text("À suivre : ${it.followUp}")
        }
    }
    CoachPanel("Nutrition liée à l'entraînement") {
        Text(if (mode(athleteSettings.nutritionMode) == CoachingMode.HUMAN)
            "Les objectifs nutritionnels saisis par votre coach sont conservés. L'IA ne les modifie pas."
        else "L'application analyse les tendances enregistrées et suggère des ajustements ; les objectifs restent soumis à validation.")
        Text("Objectif actuel : ${nutrition.calories} kcal • P ${nutrition.protein} g • G ${nutrition.carbs} g • L ${nutrition.fat} g")
        if (recordedDays < 3) Text("Seulement $recordedDays journée(s) alimentée(s) sur 7 : informations insuffisantes pour juger les apports.")
        else Text("Moyenne protéines des jours saisis : ${String.format(Locale.FRANCE,"%.0f", proteinAvg)} g (objectif ${nutrition.protein} g).")
        review.advice.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        Text("La fatigue déclarée peut faire moduler la séance. L'application ne compense pas automatiquement l'entraînement par une réduction ou un ajout de calories.",
            style = MaterialTheme.typography.bodySmall)
    }
    CoachPanel("Progression & points à améliorer") {
        val thisMonth = workoutHistory.filter { ChronoUnit.DAYS.between(it.date, today) in 0..27 }
        val groupSets = thisMonth.groupBy { it.muscle }.mapValues { item -> item.value.sumOf { it.sets } }
        if (thisMonth.isEmpty()) Text("Commencez à saisir vos entraînements pour voir quels groupes sont peu sollicités.")
        else {
            Muscle.entries.forEach { group -> Text("${group.label} : ${groupSets[group] ?: 0} séries enregistrées sur 28 jours") }
            val last14 = thisMonth.filter { ChronoUnit.DAYS.between(it.date, today) <= 13 }
            val previous14 = thisMonth.filter { ChronoUnit.DAYS.between(it.date, today) in 14..27 }
            Text("Évolution de la répartition (14 derniers jours vs 14 précédents) :", fontWeight = FontWeight.SemiBold)
            Muscle.entries.forEach { group ->
                val current = last14.filter { it.muscle == group }.sumOf { it.sets }
                val before = previous14.filter { it.muscle == group }.sumOf { it.sets }
                if (current > 0 || before > 0) Text("${group.label} : $before → $current séries consignées")
            }
            if (last14.isNotEmpty() && previous14.isNotEmpty()) {
                val lastRpe = last14.map { it.rpe }.average()
                val previousRpe = previous14.map { it.rpe }.average()
                Text("RPE moyen : ${String.format(Locale.FRANCE,"%.1f", previousRpe)} → ${String.format(Locale.FRANCE,"%.1f", lastRpe)} (ressenti déclaré, non mesure de force)")
            }
        }
        Text(metrics.take(2).let { if (it.size < 2) "Évolution du poids : au moins 2 pesées nécessaires."
            else "Écart entre les 2 dernières pesées : ${String.format(Locale.FRANCE,"%+.1f", it[0].weightKg - it[1].weightKg)} kg." })
        Text("Ces repères n'établissent pas à eux seuls un diagnostic de faiblesse ou de progrès.", style = MaterialTheme.typography.bodySmall)
    }
    if (addingExtra) ExtraWorkoutDialog(vm) { addingExtra = false }
}

private fun Meal.dayDate(): LocalDate = runCatching { LocalDate.parse(day) }.getOrDefault(LocalDate.MIN)
private fun WorkoutLog.dayDate(): LocalDate = runCatching { LocalDate.parse(day) }.getOrDefault(LocalDate.MIN)
private fun ExtraWorkout.dayDate(): LocalDate = runCatching { LocalDate.parse(day) }.getOrDefault(LocalDate.MIN)

@Composable
private fun CoachPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ExtraWorkoutDialog(vm: CoachViewModel, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var rpe by remember { mutableStateOf("7") }
    var selected by remember { mutableStateOf(Muscle.LEGS) }
    var userSelectedMuscle by remember { mutableStateOf(false) }
    var pain by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Enregistrer un exercice libre") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = {
                name = it
                if (!userSelectedMuscle) selected = detectMuscle(it) ?: selected
            }, label = { Text("Exercice réalisé") }, singleLine = true)
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (AAAA-MM-JJ)") }, singleLine = true)
            Text("Groupe musculaire principal (vérifier)")
            Muscle.entries.chunked(3).forEach { group ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    group.forEach { option -> FilterChip(selected = selected == option,
                        onClick = { selected = option; userSelectedMuscle = true }, label = { Text(option.label) }) }
                }
            }
            OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Séries") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Répétitions") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = rpe, onValueChange = { rpe = it }, label = { Text("Difficulté RPE (1 à 10)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row { Checkbox(checked = pain, onCheckedChange = { pain = it }); Text("Douleur inhabituelle ressentie", modifier = Modifier.padding(top = 14.dp)) }
        }
    }, confirmButton = { Button(onClick = {
        if (vm.logExtra(name, date, selected, sets.toIntOrNull() ?: -1, reps.toIntOrNull() ?: -1, rpe.toIntOrNull() ?: -1, pain)) onClose()
    }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } })
}
