package com.coachlink.one.domain

import kotlin.math.max

/** Estimates are for trend tracking, not a tested 1RM or training prescription. */
data class LiftSample(val name:String,val reps:Int,val kg:Double,val recordedAtMs:Long,val setType:String="NORMAL")
data class LiftStats(val bestKg:Double,val estimatedOneRm:Double,val totalVolumeKg:Double,
    val totalSets:Int,val lastReps:Int,val lastKg:Double,val history:List<LiftSample>)

fun estimatedOneRm(kg:Double,reps:Int):Double? {
    if(!kg.isFinite() || kg<=0 || reps !in 1..10) return null
    return kg * (1.0 + (reps - 1) / 30.0)
}
fun liftStats(all:List<LiftSample>, name:String):LiftStats? {
    val entries=all.filter{it.name.trim().equals(name.trim(),ignoreCase=true) &&
        it.reps>0 && it.kg>=0 && it.kg.isFinite() && it.setType!="SKIP"}
        .sortedBy{it.recordedAtMs}
    if(entries.isEmpty())return null
    val last=entries.last()
    return LiftStats(entries.maxOf{it.kg},entries.mapNotNull{estimatedOneRm(it.kg,it.reps)}.maxOrNull() ?: 0.0,
        entries.sumOf{it.kg*it.reps},entries.size,last.reps,last.kg,entries)
}
fun trainingVolume(kg:Double,reps:Int):Double =
    if(kg.isFinite() && kg>=0 && reps>=0)kg*reps else 0.0
