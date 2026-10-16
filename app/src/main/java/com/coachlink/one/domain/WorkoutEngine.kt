package com.coachlink.one.domain

import kotlin.math.max

/** Pure clock logic. A wall-clock timestamp keeps the interval coherent after app switching. */
fun sessionSeconds(startedAtMs: Long, pausedTotalMs: Long, pausedAtMs: Long?, nowMs: Long): Long {
    val end = pausedAtMs ?: nowMs
    return max(0L, (end - startedAtMs - pausedTotalMs) / 1000L)
}

fun restSecondsLeft(restStartedAtMs: Long, restRemainingSec: Int, nowMs: Long): Int {
    if (restRemainingSec <= 0) return 0
    if (restStartedAtMs <= 0L) return restRemainingSec
    return max(0, restRemainingSec - ((nowMs - restStartedAtMs).coerceAtLeast(0L) / 1000L).toInt())
}

fun blockSeconds(startedAtMs: Long, blockStartedMs: Long, totalPausedMs: Long,
                 pauseBaselineMs: Long, pausedAtMs: Long?, nowMs: Long): Long {
    if (blockStartedMs < startedAtMs) return 0L
    return sessionSeconds(blockStartedMs, (totalPausedMs - pauseBaselineMs).coerceAtLeast(0), pausedAtMs, nowMs)
}

/** Format-specific timer labels. STRENGTH/FOR_TIME run upwards; AMRAP/EMOM/TABATA count down. */
data class WodTimer(val secondsShown: Long, val overtime: Boolean, val phase: String, val round: Int)
fun wodTimer(format: String, elapsedSec: Long, capSec: Int, rounds: Int): WodTimer {
    val time = elapsedSec.coerceAtLeast(0)
    return when (format) {
        "AMRAP" -> WodTimer((capSec.toLong() - time).coerceAtLeast(0),time >= capSec && capSec > 0,"AMRAP",0)
        "EMOM" -> WodTimer(if(capSec>0)(capSec.toLong()-time).coerceAtLeast(0) else 60 - time % 60,
            capSec>0 && time>=capSec, "EMOM · minute ${time/60+1}", (time/60+1).toInt())
        "TABATA" -> {
            val limit = rounds.coerceIn(1,50) * 30L
            val cycle = time % 30L
            WodTimer((if(cycle < 20L)20L-cycle else 30L-cycle).coerceAtLeast(0), time>=limit,
                if(cycle < 20L)"EFFORT" else "REPOS",(time/30+1).toInt())
        }
        "FOR_TIME" -> WodTimer(time,capSec>0 && time>=capSec,"FOR TIME",0)
        else -> WodTimer(time,false,"FORCE / TECHNIQUE",0)
    }
}
fun clockText(seconds: Long): String = "%02d:%02d".format(seconds.coerceAtLeast(0L)/60,seconds.coerceAtLeast(0L)%60)
