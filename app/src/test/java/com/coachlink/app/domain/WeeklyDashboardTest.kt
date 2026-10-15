package com.coachlink.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyDashboardTest {
    private fun date(iso: String) = LocalDate.parse(iso)
    private fun food(day: String, kcal: Double, source: String = "manual") =
        FoodRecord(date(day), MacroValues(kcal, 30.0, 40.0, 10.0), source)

    @Test fun weekBeginsOnMondayEvenWhenReferenceIsSunday() {
        val summary = summarizeWeek(date("2026-10-18"), emptyList(), emptyList(), emptyList())
        assertEquals(date("2026-10-12"), summary.monday)
        assertEquals(date("2026-10-18"), summary.sunday)
        assertEquals(7, summary.days.size)
        assertTrue(summary.days.all { !it.hasFoodData })
        assertNull(summary.averageOnRecordedDays)
    }

    @Test fun missingDaysAreNeverCountedAsZeroConsumption() {
        val summary = summarizeWeek(date("2026-10-15"), listOf(
            food("2026-10-13", 600.0), food("2026-10-13", 400.0),
            food("2026-10-15", 2000.0), food("2026-10-05", 99999.0),
        ), emptyList(), emptyList())
        assertEquals(2, summary.recordedFoodDays)
        assertEquals(3000.0, summary.totalNutrition.calories, 0.001)
        assertEquals(1500.0, summary.averageOnRecordedDays!!.calories, 0.001)
        assertFalse(summary.days.first().hasFoodData)
        assertEquals(2, summary.days[1].foodCount)
    }

    @Test fun photoCountIsIngredientsNotSeparateMeals() {
        val summary = summarizeWeek(date("2026-10-15"), listOf(
            food("2026-10-14", 300.0, "photo_ai_confirmed"),
            food("2026-10-14", 200.0, "photo_ai_confirmed"),
            food("2026-10-14", 100.0),
        ), emptyList(), emptyList())
        assertEquals(2, summary.photoEstimatedFoodEntries)
        assertEquals(1, summary.recordedFoodDays)
    }

    @Test fun workoutDaysAndLogsAreDistinctConcepts() {
        val summary = summarizeWeek(date("2026-10-15"), emptyList(), listOf(
            ExerciseRecord(date("2026-10-13"), 3, 8, 40.0),
            ExerciseRecord(date("2026-10-13"), 4, 6, 30.0),
            ExerciseRecord(date("2026-10-16"), 5, 5, 0.0),
            ExerciseRecord(date("2026-10-05"), 20, 20, 200.0),
        ), emptyList())
        assertEquals(2, summary.exerciseDays)
        assertEquals(3, summary.exerciseLogCount)
        assertEquals(1680.0, summary.externalVolumeKg, 0.001)
    }

    @Test fun weightDeltaOnlyWhenTwoMeasuresWereMadeDuringTheWeek() {
        val metrics = listOf(
            WeightRecord(date("2026-10-10"), 104.0),
            WeightRecord(date("2026-10-12"), 103.2),
            WeightRecord(date("2026-10-16"), 102.6),
            WeightRecord(date("2026-10-21"), 101.5),
        )
        val summary = summarizeWeek(date("2026-10-15"), emptyList(), emptyList(), metrics)
        assertEquals(2, summary.weightsDuringWeek.size)
        assertEquals(-0.6, summary.withinWeekWeightChangeKg!!, 0.001)
        assertEquals(date("2026-10-16"), summary.latestKnownWeight!!.day)
        val following = summarizeWeek(date("2026-10-22"), emptyList(), emptyList(), metrics)
        assertNull(following.withinWeekWeightChangeKg)
    }
}
