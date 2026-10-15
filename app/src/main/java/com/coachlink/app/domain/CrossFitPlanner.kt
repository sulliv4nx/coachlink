package com.coachlink.app.domain

import java.time.LocalDate

/**
 * CrossFit exposure is approximated as training load indicators, never as actual gym sets.
 * Athlete-confirmed muscles take precedence over keyword detection.
 */
fun detectWodMuscles(description: String): Set<Muscle> {
    val input = description.lowercase()
        .replace('-', ' ').replace('_', ' ')
    val groups = mutableSetOf<Muscle>()
    fun addIf(vararg words: String, muscles: Set<Muscle>) {
        if (words.any { input.contains(it) }) groups.addAll(muscles)
    }
    addIf("thruster", "wall ball", "wallball", muscles = setOf(Muscle.LEGS, Muscle.SHOULDERS))
    addIf("burpee", muscles = setOf(Muscle.CHEST, Muscle.LEGS, Muscle.CARDIO))
    addIf("clean", "snatch", "épaulé", "epaule", muscles = setOf(Muscle.LEGS, Muscle.BACK, Muscle.SHOULDERS))
    addIf("jerk", "strict press", "push press", "hspu", "handstand push", muscles = setOf(Muscle.SHOULDERS))
    addIf("pull up", "pullup", "pull ups", "pull-ups", "traction", "muscle up", "chest to bar",
        "toes to bar", "t2b", "row bent", muscles = setOf(Muscle.BACK))
    addIf("toes to bar", "t2b", "sit up", "sit-up", "abdo", "plank", "gainage",
        muscles = setOf(Muscle.CORE))
    addIf("deadlift", "soulevé de terre", "souleve de terre", muscles = setOf(Muscle.BACK, Muscle.LEGS))
    addIf("squat", "lunge", "fente", "box jump", "pistol", muscles = setOf(Muscle.LEGS))
    addIf("push up", "push-up", "pompe", "bench", "dips", muscles = setOf(Muscle.CHEST))
    addIf("rowing", "rower", "rameur", "assault bike", "echo bike", "run", "course",
        "ski erg", "skierg", "bike", "double under", "corde à sauter", "corde a sauter",
        muscles = setOf(Muscle.CARDIO))
    return groups
}

fun crossfitEffortUnits(durationMinutes: Int, rpe: Int): Int {
    val baseline = when {
        durationMinutes < 15 -> 2
        durationMinutes < 35 -> 3
        durationMinutes < 60 -> 4
        else -> 5
    }
    return (baseline + if (rpe >= 8) 1 else 0).coerceIn(2, 6)
}

fun crossfitActivities(
    day: LocalDate, title: String, minutes: Int, rpe: Int,
    muscles: Set<Muscle>, pain: Boolean
): List<ActivityInput> = muscles.map { group ->
    ActivityInput(
        name = "CrossFit - " + title,
        date = day,
        muscle = group,
        sets = crossfitEffortUnits(minutes, rpe),
        reps = 1,
        rpe = rpe,
        pain = pain,
        source = "crossfit"
    )
}
