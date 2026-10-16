package com.coachlink.one.domain

data class ExercisePreset(val name:String,val group:String,val equipment:String)
val exerciseLibrary=listOf(
    ExercisePreset("Back squat","Jambes","Barre"),
    ExercisePreset("Front squat","Jambes","Barre"),
    ExercisePreset("Goblet squat","Jambes","Haltère"),
    ExercisePreset("Fentes marchées","Jambes","Haltères"),
    ExercisePreset("Bulgarian split squat","Jambes","Haltères"),
    ExercisePreset("Soulevé de terre","Dos / jambes","Barre"),
    ExercisePreset("Soulevé de terre roumain","Ischio-jambiers","Barre"),
    ExercisePreset("Hip thrust","Fessiers","Barre"),
    ExercisePreset("Leg press","Jambes","Machine"),
    ExercisePreset("Leg curl","Ischio-jambiers","Machine"),
    ExercisePreset("Développé couché","Pectoraux","Barre"),
    ExercisePreset("Développé incliné","Pectoraux","Haltères"),
    ExercisePreset("Dips","Pectoraux / triceps","Poids du corps"),
    ExercisePreset("Pompes","Pectoraux","Poids du corps"),
    ExercisePreset("Tractions strictes","Dos","Poids du corps"),
    ExercisePreset("Tractions assistées","Dos","Machine"),
    ExercisePreset("Rowing barre","Dos","Barre"),
    ExercisePreset("Rowing haltère","Dos","Haltère"),
    ExercisePreset("Tirage horizontal","Dos","Poulie"),
    ExercisePreset("Lat pulldown","Dos","Poulie"),
    ExercisePreset("Strict press","Épaules","Barre"),
    ExercisePreset("Push press","Épaules","Barre"),
    ExercisePreset("Élévations latérales","Épaules","Haltères"),
    ExercisePreset("Face pull","Épaules / dos","Poulie"),
    ExercisePreset("Curl biceps","Bras","Haltères"),
    ExercisePreset("Extension triceps","Bras","Poulie"),
    ExercisePreset("Gainage","Tronc","Poids du corps"),
    ExercisePreset("Hanging knee raises","Tronc","Poids du corps"),
    ExercisePreset("Toes to bar","Tronc / dos","Poids du corps"),
    ExercisePreset("Dead bug","Tronc","Poids du corps"),
    ExercisePreset("Thrusters","Jambes / épaules","Barre"),
    ExercisePreset("Wall balls","Jambes / épaules","Medball"),
    ExercisePreset("Power clean","Full body","Barre"),
    ExercisePreset("Clean & jerk","Full body","Barre"),
    ExercisePreset("Snatch","Full body","Barre"),
    ExercisePreset("Burpees","Full body","Poids du corps"),
    ExercisePreset("Box jumps","Jambes","Plyo box"),
    ExercisePreset("Double unders","Cardio","Corde"),
    ExercisePreset("Rameur","Cardio","Rameur"),
    ExercisePreset("Echo bike","Cardio","Bike"),
    ExercisePreset("SkiErg","Cardio","SkiErg"),
    ExercisePreset("Course à pied","Cardio","Course")
)
fun searchExercises(query:String, limit:Int=8):List<ExercisePreset> {
    val q=query.trim()
    if(q.isBlank())return exerciseLibrary.take(limit)
    return exerciseLibrary.filter{it.name.contains(q,ignoreCase=true) ||
        it.group.contains(q,ignoreCase=true) || it.equipment.contains(q,ignoreCase=true)}.take(limit)
}
