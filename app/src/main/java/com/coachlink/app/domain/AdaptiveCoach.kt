package com.coachlink.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** Local, explainable adaptation engine. It is not a trained model or clinical adviser. */
enum class Muscle(val label: String) {
    CHEST("Pectoraux"), BACK("Dos"), LEGS("Jambes"), SHOULDERS("Épaules"),
    ARMS("Bras"), CORE("Tronc"), CARDIO("Cardio")
}

enum class TrainingObjective(val label: String) {
    STRENGTH("Force"), HYPERTROPHY("Hypertrophie"), ENDURANCE("Endurance"),
    FITNESS("Forme générale"), BODY_COMP("Recomposition")
}

enum class CoachingMode(val label: String) { HUMAN("Programme du coach"), AI("Coach IA adaptatif") }

data class ActivityInput(
    val name: String,
    val date: LocalDate,
    val muscle: Muscle,
    val sets: Int,
    val reps: Int,
    val rpe: Int,
    val pain: Boolean = false,
    val source: String = "extra"
)

data class AthleteInputs(
    val objective: TrainingObjective,
    val weakMuscle: Muscle?,
    val sessionsPerWeek: Int,
    val weightKg: Double?,
    val nutritionTarget: MacroTarget,
    /** Null = no sufficiently representative nutrition data. */
    val averageProteinG: Double?,
    val recentNutritionDays: Int
)

data class PlannedExercise(val name: String, val muscle: Muscle, val sets: Int, val reps: Int, val restSec: Int)

data class SessionProposal(
    val title: String,
    val focus: Muscle?,
    val exercises: List<PlannedExercise>,
    val explanation: List<String>,
    val warnings: List<String>,
    val recentLoadScores: Map<Muscle, Double>
)

/** Conservative aliases. Unrecognised movements are labelled CORE/Other by the recording UI. */
fun detectMuscle(exercise: String): Muscle? {
    val n = exercise.trim().lowercase()
    return when {
        listOf("bench", "développé couché", "developpe couche", "pompe", "pec", "dips").any(n::contains) -> Muscle.CHEST
        listOf("traction", "pull up", "pull-up", "rowing", "tirage", "soulevé de terre", "souleve de terre", "deadlift").any(n::contains) -> Muscle.BACK
        listOf("squat", "fente", "presse", "leg ", "hip thrust", "front squat", "sdt jambes").any(n::contains) -> Muscle.LEGS
        listOf("military", "strict press", "push press", "développé militaire", "developpe militaire", "élévation latérale").any(n::contains) -> Muscle.SHOULDERS
        listOf("curl", "triceps", "biceps", "extension poulie").any(n::contains) -> Muscle.ARMS
        listOf("run", "course", "vélo", "velo", "rameur", "bike", "ski erg", "ski-erg").any(n::contains) -> Muscle.CARDIO
        listOf("gainage", "planche", "abdo", "core").any(n::contains) -> Muscle.CORE
        else -> null
    }
}

private val catalog = mapOf(
    Muscle.CHEST to listOf("Développé couché", "Pompes", "Développé incliné"),
    Muscle.BACK to listOf("Tirage horizontal", "Tractions assistées", "Rowing haltères"),
    Muscle.LEGS to listOf("Squat goblet", "Fentes arrière", "Soulevé de terre roumain"),
    Muscle.SHOULDERS to listOf("Développé épaules", "Élévations latérales", "Oiseau haltères"),
    Muscle.ARMS to listOf("Curl haltères", "Extension triceps", "Curl marteau"),
    Muscle.CORE to listOf("Gainage frontal", "Dead bug", "Pallof press"),
    Muscle.CARDIO to listOf("Marche rapide", "Vélo facile", "Rameur facile")
)

/**
 * Session priorities are recomputed on every request from recorded history.
 * - Heavy (RPE>=8) sessions get an extra 48h penalty, residual 72h penalty.
 * - Chest also taxes shoulders / arms; back taxes arms; legs get no automatic upper-body penalty.
 * - Pain or reported injury never leads to loading the affected area without assessment.
 * The algorithm does NOT diagnose injury or prescribe nutrition.
 */
fun proposeSession(now: LocalDate, athlete: AthleteInputs, history: List<ActivityInput>): SessionProposal {
    val recent = history.filter {
        !it.date.isAfter(now) && ChronoUnit.DAYS.between(it.date, now) in 0..7
    }
    val scored = Muscle.entries.associateWith { 0.0 }.toMutableMap()
    val warnings = mutableListOf<String>()
    val explanations = mutableListOf<String>()
    for (item in recent) {
        val elapsed = ChronoUnit.DAYS.between(item.date, now)
        val freshness = when (elapsed) { 0L -> 1.45; 1L -> 1.2; 2L -> .75; 3L -> .40; else -> .13 }
        val hard = if (item.rpe >= 8) 1.5 else if (item.rpe >= 6) 1.0 else .6
        val work = item.sets.coerceIn(1, 30) * freshness * hard
        scored[item.muscle] = scored.getValue(item.muscle) + work
        val overlap = when (item.muscle) {
            Muscle.CHEST -> listOf(Muscle.SHOULDERS, Muscle.ARMS)
            Muscle.BACK -> listOf(Muscle.ARMS)
            Muscle.SHOULDERS -> listOf(Muscle.ARMS)
            else -> emptyList()
        }
        overlap.forEach { scored[it] = scored.getValue(it) + work * .48 }
    }
    val painful = recent.filter { it.pain && ChronoUnit.DAYS.between(it.date, now) <= 7 }.map { it.muscle }.toSet()
    if (painful.isNotEmpty()) {
        warnings += "Douleur signalée (${painful.joinToString { it.label }}) : évitez de charger cette zone ; faites évaluer toute douleur persistante ou inhabituelle."
    }
    val yesterdayHard = recent.filter { ChronoUnit.DAYS.between(it.date, now) <= 1 && it.rpe >= 8 }
    val blocked = (yesterdayHard.map { it.muscle } + painful).toSet()
    if (yesterdayHard.isNotEmpty()) explanations += "Séances intenses récentes prises en compte : priorité aux groupes musculaires différents."
    val basicFocuses = when (athlete.objective) {
        TrainingObjective.STRENGTH -> listOf(Muscle.LEGS, Muscle.BACK, Muscle.CHEST, Muscle.SHOULDERS, Muscle.CORE)
        TrainingObjective.HYPERTROPHY -> listOf(Muscle.BACK, Muscle.LEGS, Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS)
        TrainingObjective.ENDURANCE -> listOf(Muscle.CARDIO, Muscle.CORE, Muscle.LEGS, Muscle.BACK)
        TrainingObjective.FITNESS -> listOf(Muscle.LEGS, Muscle.BACK, Muscle.CHEST, Muscle.CARDIO, Muscle.CORE)
        TrainingObjective.BODY_COMP -> listOf(Muscle.LEGS, Muscle.BACK, Muscle.CHEST, Muscle.CARDIO, Muscle.CORE)
    }
    val options = basicFocuses.filterNot { it in blocked }
    if (options.isEmpty()) {
        return SessionProposal("Repos / mobilité", null, emptyList(),
            explanations + "Aucun groupe suffisamment disponible d'après les entraînements signalés.", warnings, scored)
    }
    val focus = options.minByOrNull { group ->
        val weakBonus = if (athlete.weakMuscle == group && group !in blocked) 2.5 else 0.0
        scored.getValue(group) - weakBonus
    }!!
    val lastSame = recent.filter { it.muscle == focus }.minOfOrNull { ChronoUnit.DAYS.between(it.date, now) }
    if (lastSame != null) explanations += "Dernier travail ciblé (${focus.label}) il y a $lastSame jour(s)."
    if (athlete.weakMuscle == focus) explanations += "Ce groupe est indiqué comme point faible et ne vient pas d'être fortement sollicité."
    val complementary = when (focus) {
        Muscle.LEGS -> Muscle.CORE
        Muscle.BACK -> Muscle.CORE
        Muscle.CHEST -> Muscle.CORE
        Muscle.SHOULDERS -> Muscle.CORE
        Muscle.ARMS -> Muscle.CORE
        Muscle.CORE -> Muscle.CARDIO
        Muscle.CARDIO -> Muscle.CORE
    }
    val (sets, reps, rest) = when (athlete.objective) {
        TrainingObjective.STRENGTH -> Triple(4, 5, 150)
        TrainingObjective.HYPERTROPHY -> Triple(3, 10, 90)
        TrainingObjective.ENDURANCE -> Triple(2, 15, 60)
        TrainingObjective.FITNESS, TrainingObjective.BODY_COMP -> Triple(3, 10, 90)
    }
    val intensityMultiplier = if (scored.getValue(focus) > 8.0) .65 else 1.0
    val sameGroupEfforts = recent.filter { it.muscle == focus }.map { it.rpe }
    val adaptation = when {
        sameGroupEfforts.size >= 2 && sameGroupEfforts.average() >= 8.5 -> -1
        sameGroupEfforts.size >= 3 && sameGroupEfforts.average() <= 6.0 -> 1
        else -> 0
    }
    if (adaptation < 0) explanations += "Effort moyen récent élevé sur ${focus.label} : une série de moins proposée."
    if (adaptation > 0) explanations += "Effort récent modéré sur ${focus.label} : une série de plus proposée, si récupération correcte."
    val primarySets = max(2, (sets * intensityMultiplier).toInt() + adaptation)
    val exercises = catalog.getValue(focus).take(2).map { PlannedExercise(it, focus, primarySets, reps, rest) } +
        if (complementary !in blocked && complementary !in painful)
            listOf(PlannedExercise(catalog.getValue(complementary).first(), complementary, 2, 10, 60))
        else emptyList()
    explanations += "Objectif ${athlete.objective.label} : proposition de ${athlete.sessionsPerWeek} séance(s) / semaine, modulable selon le retour d'effort."
    if (athlete.recentNutritionDays < 3) {
        explanations += "Journal alimentaire trop incomplet pour conclure sur les apports : aucun déficit calorique présumé."
    } else if (athlete.averageProteinG != null && athlete.nutritionTarget.protein > 0 &&
        athlete.averageProteinG < athlete.nutritionTarget.protein * .8) {
        explanations += "Les apports protéiques renseignés sont sous l'objectif : vérifier les quantités avant d'intensifier la programmation."
    }
    explanations += "Proposition à valider : charges non prescrites automatiquement, adapter à la technique et à la récupération."
    return SessionProposal("Séance ${focus.label.lowercase()} • ${athlete.objective.label.lowercase()}", focus, exercises, explanations, warnings, scored)
}
