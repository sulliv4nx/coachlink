package com.coachlink.one.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SportMetricsTest {
    @Test fun goalCountsOnlyCurrentWeek() {
        val now=LocalDate.of(2026,10,16)
        val records=listOf(
            SportSessionRecord(LocalDate.of(2026,10,12),40,210),
            SportSessionRecord(LocalDate.of(2026,10,15),55,320),
            SportSessionRecord(LocalDate.of(2026,10,11),90,600))
        assertEquals(2,progressForSportGoal(records,SportGoalMetric.SESSIONS,SportGoalPeriod.WEEK,now))
        assertEquals(95,progressForSportGoal(records,SportGoalMetric.MINUTES,SportGoalPeriod.WEEK,now))
        assertEquals(530,progressForSportGoal(records,SportGoalMetric.KCAL,SportGoalPeriod.WEEK,now))
    }
    @Test fun monthlyGoalUsesCurrentMonth() {
        val now=LocalDate.of(2026,10,16)
        val records=listOf(SportSessionRecord(now,45,400), SportSessionRecord(LocalDate.of(2026,9,29),20,200))
        assertEquals(400,progressForSportGoal(records,SportGoalMetric.KCAL,SportGoalPeriod.MONTH,now))
    }
    @Test fun estimationHasBoundedInputs() {
        assertNull(estimateActivityCalories(0.0,60,"CrossFit / WOD"))
        assertNull(estimateActivityCalories(75.0,500,"Cardio"))
        assertTrue((estimateActivityCalories(80.0,40,"Musculation")?:0)>0)
    }
    @Test fun nutritionalScaling() {
        val n=toNutrition(250.0,10.0,30.0,8.0,150.0)
        assertEquals(375.0,n.kcal,0.01)
        assertEquals(15.0,n.protein,0.01)
    }
}
