package com.coachlink.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CrossFitPlannerTest {
    @Test fun detectsMultipleMuscleGroups() {
        val groups = detectWodMuscles("AMRAP 15: 12 thrusters, 10 pull-ups, 200m run")
        assertTrue(Muscle.LEGS in groups)
        assertTrue(Muscle.SHOULDERS in groups)
        assertTrue(Muscle.BACK in groups)
        assertTrue(Muscle.CARDIO in groups)
    }
    @Test fun noFalsePositiveForUnknownWod() {
        assertTrue(detectWodMuscles("WOD inconnu").isEmpty())
    }
    @Test fun preservesMultipleExposureEntries() {
        val day = LocalDate.of(2026, 10, 15)
        val acts = crossfitActivities(day, "Fran", 15, 9, setOf(Muscle.LEGS, Muscle.BACK), false)
        assertEquals(2, acts.size)
        assertTrue(acts.all { it.source == "crossfit" && it.rpe == 9 })
        assertTrue(acts.all { it.date == day })
    }
    @Test fun intenseWodReducesRepeatForTheNextDay() {
        val today = LocalDate.of(2026, 10, 16)
        val wod = crossfitActivities(today.minusDays(1), "Fran", 40, 9,
            setOf(Muscle.LEGS, Muscle.SHOULDERS), false)
        val profile = AthleteInputs(TrainingObjective.FITNESS, Muscle.LEGS, 4, null,
            MacroTarget(2400, 150, 280, 70), null, 0)
        val proposal = proposeSession(today, profile, wod)
        assertTrue(proposal.focus != Muscle.LEGS)
        assertTrue(proposal.focus != Muscle.SHOULDERS)
    }
}
