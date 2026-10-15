package com.coachlink.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coachlink.app.data.BodyMetric
import com.coachlink.app.data.Goal
import com.coachlink.app.data.Meal
import com.coachlink.app.data.WorkoutLog
import com.coachlink.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val french = Locale.FRANCE
private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", french)
private val weekGreen = Color(0xFF16635D)
private val warmAccent = Color(0xFFC3863E)

/** Screen read-only: the same single local athlete dataset powers the coach and athlete views. */
@Composable
fun WeeklyDashboardScreen(
    goal: Goal,
    meals: List<Meal>,
    logs: List<WorkoutLog>,
    metrics: List<BodyMetric>,
    coach: Boolean,
) {
    val today = LocalDate.now()
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var exampleMode by rememberSaveable { mutableStateOf(false) }
    val reference = today.plusWeeks(weekOffset.toLong())
    val week = remember(meals, logs, metrics, reference, exampleMode) {
        if (exampleMode) exampleWeek(reference) else summarizeWeek(
            reference = reference,
            foods = meals.mapNotNull { m ->
                runCatching {
                    FoodRecord(
                        LocalDate.parse(m.day),
                        scaleFood(MacroValues(m.calories100, m.protein100, m.carbs100, m.fat100), m.grams),
                        m.source,
                    )
                }.getOrNull()
            },
            exercises = logs.mapNotNull { log ->
                runCatching { ExerciseRecord(LocalDate.parse(log.day), log.setsDone, log.repsDone, log.loadKg) }.getOrNull()
            },
            weights = metrics.mapNotNull { metric ->
                runCatching { WeightRecord(LocalDate.parse(metric.day), metric.weightKg) }.getOrNull()
            },
        )
    }

    Text(
        if (coach) "Suivi hebdomadaire du dossier" else "Mon tableau de bord hebdomadaire",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { weekOffset-- }, enabled = weekOffset > -52) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Semaine précédente")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${week.monday.format(dateFormat)} – ${week.sunday.format(dateFormat)}", fontWeight = FontWeight.SemiBold)
            Text(if (weekOffset == 0) "Semaine en cours" else "Semaine passée", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { weekOffset++ }, enabled = weekOffset < 0) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Semaine suivante")
        }
    }

    OutlinedButton(onClick = { exampleMode = !exampleMode }, modifier = Modifier.fillMaxWidth()) {
        Text(if (exampleMode) "Quitter l'aperçu fictif" else "Afficher un exemple de tableau rempli")
    }
    if (exampleMode) {
        Text("DONNÉES FICTIVES — aperçu uniquement, aucun repas ou entraînement enregistré.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Nutrition • 7 jours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${week.recordedFoodDays} / 7 jours avec alimentation renseignée", fontWeight = FontWeight.Medium)
            WeeklyCalorieBars(week, goal.calories)
            val avg = week.averageOnRecordedDays
            if (avg == null) {
                Text("Aucun repas enregistré sur cette semaine.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Moyenne par jour renseigné", fontWeight = FontWeight.SemiBold)
                TargetComparison("Calories", avg.calories, goal.calories.toDouble(), "kcal")
                TargetComparison("Protéines", avg.protein, goal.protein.toDouble(), "g")
                TargetComparison("Glucides", avg.carbs, goal.carbs.toDouble(), "g")
                TargetComparison("Lipides", avg.fat, goal.fat.toDouble(), "g")
            }
            Text("${week.photoEstimatedFoodEntries} ingrédient(s) issu(s) de photos IA validées",
                style = MaterialTheme.typography.bodySmall)
            Text("Un jour non renseigné n'est pas compté comme 0 kcal. Les valeurs des photos restent des estimations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Activité sportive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${week.exerciseDays} jour(s) avec activité enregistrée")
            Text("${week.exerciseLogCount} exercice(s) consignés")
            Text("Volume en charges externes : ${week.externalVolumeKg.roundToInt()} kg")
            Text("Le nombre d'exercices ne représente pas le nombre de séances. Les mouvements au poids du corps n'ajoutent pas de charge externe.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Mesures corporelles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val last = week.latestKnownWeight
            Text(if (last == null) "Aucune pesée connue jusqu'à cette semaine"
                else "Dernière pesée connue : ${formatOne(last.weightKg)} kg (${last.day.format(dateFormat)})")
            Text("${week.weightsDuringWeek.size} pesée(s) sur la semaine")
            week.withinWeekWeightChangeKg?.let { delta ->
                Text("Variation entre deux pesées de cette semaine : ${if (delta > 0) "+" else ""}${formatOne(delta)} kg")
            }
        }
    }
    if (coach) {
        Text("Lecture coach : consulter les jours sans saisie et l'historique avant d'ajuster un programme. Les objectifs actuels sont appliqués aux semaines passées dans cette démonstration.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyCalorieBars(week: WeeklyDashboardData, dailyGoal: Int) {
    // A full-height bar represents 150 % of today's target; the exact kcal number remains visible.
    Text("Apports par jour (barre pleine = 150 % de l'objectif)", style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        week.days.forEach { day ->
            val ratio = if (dailyGoal > 0) day.nutrition.calories / dailyGoal else 0.0
            val progress = (ratio / 1.5).toFloat().coerceIn(0f, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (day.hasFoodData) day.nutrition.calories.roundToInt().toString() else "—",
                    style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(Modifier.fillMaxWidth().height(90.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.BottomCenter) {
                    if (day.hasFoodData) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(progress.coerceAtLeast(0.035f))
                            .background(if (ratio > 1.1) warmAccent else weekGreen, RoundedCornerShape(5.dp)))
                    } else {
                        Box(Modifier.fillMaxSize().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(5.dp)))
                    }
                }
                Text(day.day.dayOfWeek.getDisplayName(TextStyle.SHORT, french).take(3),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TargetComparison(label: String, observed: Double, target: Double, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${formatOne(observed)} / ${formatOne(target)} $unit", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { adherence(observed, target) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = if (observed > target * 1.1 && target > 0) warmAccent else weekGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (target > 0 && observed > target) {
            Text("Dépassement de ${formatOne(observed - target)} $unit", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatOne(value: Double): String = String.format(french, "%.1f", value)


/** Fictitious preview only: nothing is ever written to Room by this path. */
private fun exampleWeek(reference: LocalDate): WeeklyDashboardData {
    val mon = firstDayOfWeek(reference)
    val f = listOf(
        FoodRecord(mon, MacroValues(730.0, 54.0, 68.0, 21.0)),
        FoodRecord(mon, MacroValues(1370.0, 91.0, 132.0, 52.0)),
        FoodRecord(mon.plusDays(1), MacroValues(2320.0, 150.0, 236.0, 75.0), "photo_ai_confirmed"),
        FoodRecord(mon.plusDays(3), MacroValues(1850.0, 127.0, 188.0, 59.0)),
        FoodRecord(mon.plusDays(4), MacroValues(2190.0, 151.0, 239.0, 69.0)),
        FoodRecord(mon.plusDays(5), MacroValues(2450.0, 148.0, 270.0, 78.0)),
    )
    val e = listOf(
        ExerciseRecord(mon, 4, 8, 60.0),
        ExerciseRecord(mon, 3, 10, 30.0),
        ExerciseRecord(mon.plusDays(3), 4, 6, 80.0),
    )
    val w = listOf(
        WeightRecord(mon, 103.8),
        WeightRecord(mon.plusDays(5), 103.4),
    )
    return summarizeWeek(reference, f, e, w)
}
