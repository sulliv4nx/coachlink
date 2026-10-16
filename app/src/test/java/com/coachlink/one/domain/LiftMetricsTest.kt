package com.coachlink.one.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LiftMetricsTest {
    @Test fun estimatesFromTenOrLessRepsOnly() {
        assertEquals(100.0,estimatedOneRm(100.0,1)!!,0.001)
        assertEquals(123.333333,estimatedOneRm(100.0,8)!!,0.001)
        assertNull(estimatedOneRm(100.0,15))
        assertNull(estimatedOneRm(0.0,5))
    }
    @Test fun historyIgnoresSkippedAndCaseMatches() {
        val a=liftStats(listOf(LiftSample("squat",5,80.0,1),LiftSample("Squat",8,60.0,2),
            LiftSample("Squat",0,0.0,3,"SKIP")), "SQUAT")!!
        assertEquals(80.0,a.bestKg,0.001)
        assertEquals(880.0,a.totalVolumeKg,0.001)
        assertEquals(2,a.totalSets)
        assertEquals(60.0,a.lastKg,0.001)
    }
    @Test fun presetsSearchesMuscleAndName() {
        assertTrue(searchExercises("thruster").any{it.name=="Thrusters"})
        assertTrue(searchExercises("SkiErg").isNotEmpty())
        assertTrue(searchExercises("épaules").isNotEmpty())
    }
}
