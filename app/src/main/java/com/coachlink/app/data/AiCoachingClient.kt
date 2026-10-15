package com.coachlink.app.data

import com.coachlink.app.BuildConfig
import com.coachlink.app.domain.ActivityInput
import com.coachlink.app.domain.AthleteInputs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AiCoachAdvice(val overview: String, val training: String, val nutrition: String, val followUp: String)

/** Optional, remote language-model explanation of an already computed local plan.
 * Only call after the athlete explicitly consents. Never ship provider credentials in Android.
 */
class AiCoachingClient {
    val configured: Boolean = BuildConfig.PHOTO_API_URL.startsWith("https://")

    suspend fun review(now: LocalDate, athlete: AthleteInputs, history: List<ActivityInput>): Result<AiCoachAdvice> = withContext(Dispatchers.IO) {
        runCatching {
            require(configured) { "Serveur IA non configuré. Le coach adaptatif local reste disponible." }
            val recent = history.filter { !it.date.isAfter(now) && ChronoUnit.DAYS.between(it.date, now) <= 14 }.take(35)
            val req = JSONObject().apply {
                put("objective", athlete.objective.label)
                put("weak_muscle", athlete.weakMuscle?.label ?: "Aucun")
                put("sessions_per_week", athlete.sessionsPerWeek)
                put("calories_target", athlete.nutritionTarget.calories)
                put("protein_target", athlete.nutritionTarget.protein)
                put("recent_nutrition_days", athlete.recentNutritionDays)
                if (athlete.averageProteinG != null) put("average_protein", athlete.averageProteinG)
                put("workouts", JSONArray().apply {
                    recent.forEach { entry ->
                        put(JSONObject().apply {
                            put("name", entry.name.take(100)); put("muscle", entry.muscle.label)
                            put("days_ago", ChronoUnit.DAYS.between(entry.date, now).toInt())
                            put("sets", entry.sets); put("reps", entry.reps)
                            put("rpe", entry.rpe); put("pain", entry.pain)
                        })
                    }
                })
            }
            val apiUrl = BuildConfig.PHOTO_API_URL.trimEnd('/') + "/v1/coach/review"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000; readTimeout = 55000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            try {
                conn.outputStream.use { it.write(req.toString().toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) error("Le coach en ligne est momentanément indisponible (HTTP ${conn.responseCode}).")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                AiCoachAdvice(json.getString("overview"), json.getString("training"),
                    json.getString("nutrition"), json.getString("follow_up"))
            } finally { conn.disconnect() }
        }
    }
}
