package com.coachlink.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.coachlink.app.BuildConfig
import com.coachlink.app.domain.MacroValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Server response: estimates only; never silently auto-save nutrition to the journal. */
data class PhotoFood(
    val name: String,
    val grams: Double,
    val per100: MacroValues,
    val confidence: String
)

data class PhotoMealEstimate(val foods: List<PhotoFood>, val notes: String)

/** Decode a sampled image to avoid retaining a full-resolution camera bitmap in memory. */
fun decodeMealPhoto(resolver: ContentResolver, uri: Uri, maxSize: Int = 1280): Bitmap {
    require(maxSize in 256..2048)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            require(w > 0 && h > 0) { "Photo illisible" }
            val scale = minOf(1f, maxSize.toFloat() / maxOf(w, h))
            decoder.setTargetSize(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt()))
        }
    }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Impossible de lire la photo")
    require(options.outWidth > 0 && options.outHeight > 0) { "Photo illisible" }
    var sample = 1
    while (maxOf(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("Impossible de décoder la photo")
    return decoded
}

class PhotoMealAnalysisClient {
    val isConfigured: Boolean = BuildConfig.PHOTO_API_URL.startsWith("https://")

    suspend fun analyze(resolver: ContentResolver, photoUri: Uri): Result<PhotoMealEstimate> =
        withContext(Dispatchers.IO) {
            runCatching {
                check(isConfigured) {
                    "Analyse IA non configurée : renseigner une URL HTTPS pour le serveur CoachLink."
                }
                val bitmap = decodeMealPhoto(resolver, photoUri)
                val encoded = ByteArrayOutputStream().use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) { "Compression impossible" }
                    out.toByteArray()
                }
                val url = URL(BuildConfig.PHOTO_API_URL.trimEnd('/') + "/v1/photo-meals/analyze")
                val c = url.openConnection() as HttpURLConnection
                try {
                    c.requestMethod = "POST"
                    c.connectTimeout = 12_000
                    c.readTimeout = 65_000
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "image/jpeg")
                    c.setRequestProperty("Accept", "application/json")
                    c.setRequestProperty("Cache-Control", "no-store")
                    c.outputStream.use { it.write(encoded) }
                    val response = c.responseCode
                    check(response == 200) {
                        "Analyse indisponible (HTTP $response). Vérifiez le serveur et réessayez."
                    }
                    val payload = c.inputStream.bufferedReader().use { it.readText() }
                    parseEstimate(JSONObject(payload))
                } finally { c.disconnect() }
            }
        }
}

fun parseEstimate(obj: JSONObject): PhotoMealEstimate {
    val array = obj.optJSONArray("foods") ?: error("Réponse IA incomplète")
    require(array.length() in 1..20) { "Aucun aliment reconnu : utilisez la saisie manuelle" }
    val foods = (0 until array.length()).map { index ->
        val f = array.getJSONObject(index)
        fun amount(key: String, min: Double, max: Double): Double {
            val v = f.optDouble(key, Double.NaN)
            require(v.isFinite() && v in min..max) { "Estimation nutritionnelle invalide ($key)" }
            return v
        }
        val name = f.optString("name").trim().take(100)
        require(name.isNotBlank()) { "Nom d'aliment manquant" }
        PhotoFood(
            name = name,
            grams = amount("grams", 1.0, 5000.0),
            per100 = MacroValues(
                calories = amount("calories100", 0.0, 900.0),
                protein = amount("protein100", 0.0, 100.0),
                carbs = amount("carbs100", 0.0, 100.0),
                fat = amount("fat100", 0.0, 100.0)
            ),
            confidence = f.optString("confidence", "low").takeIf { it in setOf("low", "medium", "high") } ?: "low"
        )
    }
    return PhotoMealEstimate(foods, obj.optString("notes").take(600))
}
