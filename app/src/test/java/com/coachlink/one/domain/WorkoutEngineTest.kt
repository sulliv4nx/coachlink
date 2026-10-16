package com.coachlink.one.domain

import org.junit.Assert.*
import org.junit.Test

class WorkoutEngineTest {
    @Test fun clockSurvivesPause() {
        assertEquals(70L,sessionSeconds(1_000L,10_000L,null,81_000L))
        assertEquals(49L,sessionSeconds(1_000L,0L,50_000L,200_000L))
    }
    @Test fun restPersistedAndPauseWorks() {
        assertEquals(70, restSecondsLeft(10_000L,90,30_000L))
        assertEquals(70, restSecondsLeft(0L,70,200_000L))
        assertEquals(0, restSecondsLeft(10_000L,90,200_000L))
    }
    @Test fun blocksIgnorePausesBeforeBlock() {
        assertEquals(30L,blockSeconds(1000, 50000, 20000, 20000, null, 80000))
        assertEquals(5L,blockSeconds(1000, 50000, 22000, 20000, null, 57000))
    }
    @Test fun amrapAndEmomWork() {
        assertEquals(300L,wodTimer("AMRAP",300,600,0).secondsShown)
        assertTrue(wodTimer("AMRAP",605,600,0).overtime)
        assertEquals(3,wodTimer("EMOM",120,600,0).round)
    }
    @Test fun tabataIntervals() {
        assertEquals("EFFORT",wodTimer("TABATA",0,0,8).phase)
        assertEquals("REPOS",wodTimer("TABATA",22,0,8).phase)
        assertEquals(2,wodTimer("TABATA",30,0,8).round)
        assertTrue(wodTimer("TABATA",241,0,8).overtime)
    }
    @Test fun manualTimeIsStopwatch() {
        assertEquals(97L,wodTimer("FOR_TIME",97,600,0).secondsShown)
        assertEquals("FORCE / TECHNIQUE",wodTimer("STRENGTH",0,0,0).phase)
    }
}
